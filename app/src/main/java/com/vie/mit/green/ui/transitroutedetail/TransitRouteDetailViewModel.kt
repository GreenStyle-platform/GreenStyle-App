package com.vie.mit.green.ui.transitroutedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.domain.map.RoundTrip
import com.vie.mit.data.repository.transit.TransitRouteRepository
import com.vie.mit.green.navigation.main_graph.TransitRouteDetailRoute
import com.vie.mit.green.navigation.main_graph.TransitStopDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TransitRouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transitRouteRepository: TransitRouteRepository,
    private val appNavigator: AppNavigator,
) : ViewModel() {

    private val routeArgs: TransitRouteDetailRoute? = runCatching {
        savedStateHandle.toRoute<TransitRouteDetailRoute>()
    }.getOrNull()

    val routeId: String = routeArgs?.routeId?.ifBlank { "" }?:""

    val typeId: TransitMode = routeArgs?.transitMode
        ?: savedStateHandle.get<TransitMode>("transitMode")
        ?: savedStateHandle.get<String>("transitMode")?.let { name ->
            runCatching { TransitMode.valueOf(name) }.getOrNull()
        }
        ?: savedStateHandle.get<String>("typeId")?.let { name ->
            runCatching { TransitMode.valueOf(name) }.getOrNull()
        }
        ?: TransitMode.BUS

    // Uses PublicTransportType.name as mode string (e.g. "bus", "subway")
    val mode: String = typeId.name.lowercase()

    private val _uiState = MutableStateFlow(
        TransitRouteDetailUiState(
            routeId = routeId,
            typeId = typeId,
            isLoading = true,
        )
    )
    val uiState: StateFlow<TransitRouteDetailUiState> = _uiState.asStateFlow()

    init {
        loadRouteTrips()
    }

    /**
     * Loads trips for the given route, groups them by [RoundTrip.serviceId],
     * and automatically triggers loading shape and stop details for the initial trip.
     */
    fun loadRouteTrips() {
        if (routeId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Mã tuyến không hợp lệ") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = transitRouteRepository.getRouteTrips(mode = mode, routeId = routeId)
            result.onSuccess { trips ->
                if (trips.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            roundTrips = emptyList(),
                            shapes = emptyList(),
                            stops = emptyList(),
                        )
                    }
                    return@launch
                }

                // Group trips by serviceId to form RoundTrip list
                val groupedRoundTrips = trips.groupBy { it.serviceId }.map { (serviceId, serviceTrips) ->
                    val outbound = serviceTrips.firstOrNull { it.directionId == 0 }
                    val inbound = serviceTrips.firstOrNull { it.directionId == 1 }
                    val anyTrip = serviceTrips.firstOrNull()
                    RoundTrip(
                        serviceId = serviceId,
                        outboundTrip = outbound,
                        inboundTrip = inbound,
                        routeId = anyTrip?.routeId.orEmpty(),
                        routeShortName = anyTrip?.routeShortName.orEmpty(),
                        routeLongName = anyTrip?.routeLongName.orEmpty(),
                    )
                }

                val firstRoundTrip = groupedRoundTrips.firstOrNull()
                val defaultServiceId = firstRoundTrip?.serviceId
                val defaultDirection = 0 // Chiều đi
                val defaultTripId = firstRoundTrip?.getTrip(defaultDirection)?.tripId
                    ?: firstRoundTrip?.inboundTrip?.tripId
                    ?: trips.firstOrNull()?.tripId

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        roundTrips = groupedRoundTrips,
                        selectedServiceId = defaultServiceId,
                        selectedDirectionId = defaultDirection,
                        selectedTripId = defaultTripId,
                        error = null,
                    )
                }

                if (!defaultTripId.isNullOrBlank()) {
                    loadTripDetails(defaultTripId)
                }
            }.onFailure { throwable ->
                Timber.e(throwable, "Failed to load route trips for routeId: $routeId")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Không thể tải danh sách chuyến đi",
                    )
                }
            }
        }
    }

    /**
     * Loads coordinate shape points for a specific trip.
     */
    fun loadTripShape(tripId: String) {
        _uiState.update { it.copy(isShapeLoading = true) }
        viewModelScope.launch {
            val result = transitRouteRepository.getTripShape(mode = mode, tripId = tripId)
            result.onSuccess { shapePoints ->
                _uiState.update { it.copy(shapes = shapePoints, isShapeLoading = false) }
            }.onFailure { throwable ->
                Timber.e(throwable, "Failed to load trip shape for tripId: $tripId")
                _uiState.update { it.copy(shapes = emptyList(), isShapeLoading = false) }
            }
        }
    }

    /**
     * Loads transit stops for a specific trip.
     */
    fun loadTripStops(tripId: String) {
        _uiState.update { it.copy(isStopsLoading = true) }
        viewModelScope.launch {
            val result = transitRouteRepository.getTripStops(mode = mode, tripId = tripId)
            result.onSuccess { stopMarkers ->
                _uiState.update { it.copy(stops = stopMarkers, isStopsLoading = false) }
            }.onFailure { throwable ->
                Timber.e(throwable, "Failed to load trip stops for tripId: $tripId")
                _uiState.update { it.copy(stops = emptyList(), isStopsLoading = false) }
            }
        }
    }

    /**
     * Concurrently loads both shape points and stops for [tripId].
     */
    fun loadTripDetails(tripId: String) {
        _uiState.update {
            it.copy(
                selectedTripId = tripId,
                isShapeLoading = true,
                isStopsLoading = true,
            )
        }
        viewModelScope.launch {
            val shapeDeferred = async { transitRouteRepository.getTripShape(mode = mode, tripId = tripId) }
            val stopsDeferred = async { transitRouteRepository.getTripStops(mode = mode, tripId = tripId) }

            val shapeResult = shapeDeferred.await()
            val stopsResult = stopsDeferred.await()

            val shapes = shapeResult.getOrElse { throwable ->
                Timber.e(throwable, "Failed to load trip shape for tripId: $tripId")
                emptyList()
            }

            val stops = stopsResult.getOrElse { throwable ->
                Timber.e(throwable, "Failed to load trip stops for tripId: $tripId")
                emptyList()
            }

            _uiState.update {
                it.copy(
                    shapes = shapes,
                    stops = stops,
                    isShapeLoading = false,
                    isStopsLoading = false,
                )
            }
        }
    }

    /**
     * Switches direction between outbound (0) and inbound (1).
     */
    fun onSelectDirection(directionId: Int) {
        val currentState = _uiState.value
        if (currentState.selectedDirectionId == directionId) return

        val roundTrip = currentState.selectedRoundTrip
        val trip = roundTrip?.getTrip(directionId)
        val tripId = trip?.tripId

        _uiState.update {
            it.copy(
                selectedDirectionId = directionId,
                selectedTripId = tripId,
            )
        }

        if (!tripId.isNullOrBlank()) {
            loadTripDetails(tripId)
        } else {
            _uiState.update { it.copy(shapes = emptyList(), stops = emptyList()) }
        }
    }

    /**
     * Switches the service schedule (e.g. weekdays vs weekends).
     */
    fun onSelectService(serviceId: String) {
        val currentState = _uiState.value
        if (currentState.selectedServiceId == serviceId) return

        val roundTrip = currentState.roundTrips.firstOrNull { it.serviceId == serviceId }
        val trip = roundTrip?.getTrip(currentState.selectedDirectionId)
            ?: roundTrip?.outboundTrip
            ?: roundTrip?.inboundTrip
        val tripId = trip?.tripId
        val direction = trip?.directionId ?: currentState.selectedDirectionId

        _uiState.update {
            it.copy(
                selectedServiceId = serviceId,
                selectedDirectionId = direction,
                selectedTripId = tripId,
            )
        }

        if (!tripId.isNullOrBlank()) {
            loadTripDetails(tripId)
        } else {
            _uiState.update { it.copy(shapes = emptyList(), stops = emptyList()) }
        }
    }

    /**
     * Selects a specific trip directly by its [tripId].
     */
    fun onSelectTrip(tripId: String) {
        val currentState = _uiState.value
        if (currentState.selectedTripId == tripId) return

        val matchedRoundTrip = currentState.roundTrips.firstOrNull { rt ->
            rt.outboundTrip?.tripId == tripId || rt.inboundTrip?.tripId == tripId
        }
        val direction = when {
            matchedRoundTrip?.outboundTrip?.tripId == tripId -> 0
            matchedRoundTrip?.inboundTrip?.tripId == tripId -> 1
            else -> currentState.selectedDirectionId
        }

        _uiState.update {
            it.copy(
                selectedTripId = tripId,
                selectedDirectionId = direction,
                selectedServiceId = matchedRoundTrip?.serviceId ?: it.selectedServiceId,
            )
        }

        loadTripDetails(tripId)
    }

    fun navigateToTransitStopDetail(stopId: String) {
        if (stopId.isBlank()) return
        appNavigator.navigateTo(
            TransitStopDetailRoute(
                stopId = stopId,
                transitMode = typeId,
            )
        ) {
            launchSingleTop = true
        }
    }

    fun navigateBack() {
        appNavigator.navigateUp()
    }
}
