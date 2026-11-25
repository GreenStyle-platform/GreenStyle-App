package com.vie.mit.green.ui.home.nearbybusstops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.repository.map.MapRepository
import com.vie.mit.green.navigation.main_graph.TransitRouteDetailRoute
import com.vie.mit.green.navigation.main_graph.TransitStopDetailRoute
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyStopItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the NearbyBusStopsSheet screen.
 *
 * Responsibilities:
 * - Receives a list of nearby bus stops from the parent (HomeContainerViewModel via SheetNavHost)
 * - Sends stop IDs to MapRepository to fetch routes passing through those stops
 * - Maps returned routes to each stop using stopId
 * - Exposes a combined UiState: list of stops with their routes and arrival times
 */
@HiltViewModel
class NearbyBusStopViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val mapRepository: MapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyBusStopsUiState())
    val uiState: StateFlow<NearbyBusStopsUiState> = _uiState.asStateFlow()

    private var loadArrivalsJob: Job? = null
    private var lastLoadedStopIds: Set<String> = emptySet()

    /**
     * Initializes the screen with a list of nearby bus stops and loads routes & arrivals
     * for all stops in a single batch request.
     *
     * When [forceReload] is false and the stop ID set has not changed (e.g. only walking distances
     * or sort order updated on minor map pan), preserves existing loaded arrivals in place without
     * triggering redundant network requests or loading flashes.
     *
     * @param busStops Nearby bus stops filtered from the transit data.
     * @param forceReload Whether to force re-fetching arrivals even when stop IDs are unchanged.
     */
    fun loadBusStops(
        busStops: List<NearbyStopItem>,
        forceReload: Boolean = false,
    ) {
        Timber.d("PhucTH: loadBusStops called (count=${busStops.size}, forceReload=$forceReload)")
        if (busStops.isEmpty()) {
            loadArrivalsJob?.cancel()
            loadArrivalsJob = null
            lastLoadedStopIds = emptySet()
            _uiState.value = NearbyBusStopsUiState()
            return
        }

        val incomingStopIds = busStops.map { it.stopId }.filter { it.isNotBlank() }.toSet()
        val existingById = _uiState.value.busStopsWithArrivals.associateBy { it.stop.stopId }

        if (!forceReload && incomingStopIds == lastLoadedStopIds && existingById.isNotEmpty()) {
            val reorderedItems = busStops.map { stop ->
                val existing = existingById[stop.stopId]
                existing?.copy(stop = stop) ?: NearbyBusStopWithArrivals(
                    stop = stop,
                    isLoadingArrivals = _uiState.value.isLoadingArrivals,
                )
            }
            _uiState.update { it.copy(busStopsWithArrivals = reorderedItems) }
            return
        }

        loadArrivalsJob?.cancel()
        lastLoadedStopIds = incomingStopIds

        // 1. Immediately show stops — preserve already-fetched routes for overlapping stops while loading
        val initialItems = busStops.map { stop ->
            val existing = existingById[stop.stopId]
            if (!forceReload && existing != null && !existing.isLoadingArrivals && existing.arrivalsError == null) {
                existing.copy(stop = stop)
            } else {
                NearbyBusStopWithArrivals(stop = stop, isLoadingArrivals = true)
            }
        }
        _uiState.update {
            it.copy(
                busStopsWithArrivals = initialItems,
                isLoadingArrivals = true,
                errorMessage = null,
            )
        }

        loadArrivalsJob = viewModelScope.launch {
            try {
                val stopIds = busStops.map { it.stopId }.filter { it.isNotBlank() }.distinct()
                if (stopIds.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            busStopsWithArrivals = busStops.map { stop ->
                                NearbyBusStopWithArrivals(stop = stop, isLoadingArrivals = false)
                            },
                            isLoadingArrivals = false,
                        )
                    }
                    return@launch
                }

                mapRepository.getBusStopArrivals(stopIds)
                    .onSuccess { allRoutes ->
                        // 2. Group returned routes by stopId
                        val routesByStopId = allRoutes.groupBy { it.stopId }

                        // 3. Map into the original busStops list (preserving distance order)
                        val updatedItems = busStops.map { stop ->
                            NearbyBusStopWithArrivals(
                                stop = stop,
                                routes = routesByStopId[stop.stopId] ?: emptyList(),
                                isLoadingArrivals = false,
                                arrivalsError = null,
                            )
                        }

                        _uiState.update {
                            it.copy(
                                busStopsWithArrivals = updatedItems,
                                isLoadingArrivals = false,
                                errorMessage = null,
                            )
                        }
                        Timber.d("NearbyBusStopViewModel: loaded ${allRoutes.size} routes for ${busStops.size} bus stops")
                    }
                    .onFailure { error ->
                        Timber.e(error, "NearbyBusStopViewModel: failed to load arrivals")
                        val updatedItems = busStops.map { stop ->
                            NearbyBusStopWithArrivals(
                                stop = stop,
                                routes = emptyList(),
                                isLoadingArrivals = false,
                                arrivalsError = error.message ?: "Không thể tải lịch trình",
                            )
                        }
                        _uiState.update {
                            it.copy(
                                busStopsWithArrivals = updatedItems,
                                isLoadingArrivals = false,
                                errorMessage = error.message ?: "Không thể tải thông tin chuyến xe",
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "NearbyBusStopViewModel: failed to load arrivals")
                val errorText = e.message ?: "Không thể tải thông tin chuyến xe"
                val updatedItems = busStops.map { stop ->
                    NearbyBusStopWithArrivals(
                        stop = stop,
                        routes = emptyList(),
                        isLoadingArrivals = false,
                        arrivalsError = errorText,
                    )
                }
                _uiState.update {
                    it.copy(
                        busStopsWithArrivals = updatedItems,
                        isLoadingArrivals = false,
                        errorMessage = errorText,
                    )
                }
            }
        }
    }

    /**
     * Refreshes arrivals for all currently displayed stops.
     * Called when user manually refreshes or when the screen re-appears.
     */
    fun refresh() {
        Timber.d("PhucTH: refresh called")
        val currentStops = _uiState.value.busStopsWithArrivals.map { it.stop }
        loadBusStops(currentStops, forceReload = true)
    }

    fun navigateToTransitStopDetail(stopId: String) {
        if (stopId.isBlank()) return
        appNavigator.navigateTo(
            TransitStopDetailRoute(
                stopId = stopId,
                transitMode = TransitMode.BUS,
            )
        ) {
            launchSingleTop = true
        }
    }

    fun navigateToTransitRouteDetail(routeId: String) {
        if (routeId.isBlank()) return
        appNavigator.navigateTo(
            TransitRouteDetailRoute(
                routeId = routeId,
                transitMode = TransitMode.BUS,
            )
        ) {
            launchSingleTop = true
        }
    }
}
