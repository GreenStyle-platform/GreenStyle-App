package com.vie.mit.green.ui.home.nearbybusstops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.data.repository.map.MapRepository
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyStopItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    private val mapRepository: MapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyBusStopsUiState())
    val uiState: StateFlow<NearbyBusStopsUiState> = _uiState.asStateFlow()

    /**
     * Initializes the screen with a list of nearby bus stops and loads routes & arrivals
     * for all stops in a single batch request.
     *
     * @param busStops Nearby bus stops filtered from the transit data.
     */
    fun loadBusStops(busStops: List<NearbyStopItem>) {
        if (busStops.isEmpty()) {
            _uiState.value = NearbyBusStopsUiState()
            return
        }

        // 1. Immediately show stops with loading placeholders
        val initialItems = busStops.map { stop ->
            NearbyBusStopWithArrivals(stop = stop, isLoadingArrivals = true)
        }
        _uiState.update {
            it.copy(
                busStopsWithArrivals = initialItems,
                isLoadingArrivals = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            try {
                val stopIds = busStops.map { it.stopId }.filter { it.isNotBlank() }
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
                                arrivalsError = error.message,
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
                _uiState.update {
                    it.copy(
                        isLoadingArrivals = false,
                        errorMessage = e.message ?: "Không thể tải thông tin chuyến xe",
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
        val currentStops = _uiState.value.busStopsWithArrivals.map { it.stop }
        loadBusStops(currentStops)
    }
}
