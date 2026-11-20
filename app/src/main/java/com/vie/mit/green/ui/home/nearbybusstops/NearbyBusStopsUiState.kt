package com.vie.mit.green.ui.home.nearbybusstops

import com.vie.mit.data.model.domain.map.StopArrival
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyStopItem

/**
 * Wraps a nearby bus stop with its loaded route arrival schedules.
 *
 * @param stop The bus stop metadata (name, distance, location)
 * @param routes Routes passing by this stop, each containing arrival times
 * @param isLoadingArrivals Whether arrivals are still being fetched for this stop
 * @param arrivalsError Non-null if fetching arrivals failed for this stop
 */
data class NearbyBusStopWithArrivals(
    val stop: NearbyStopItem,
    val routes: List<StopArrival> = emptyList(),
    val isLoadingArrivals: Boolean = true,
    val arrivalsError: String? = null,
) {
    // Backward compatibility helper
    val arrivals: List<StopArrival> get() = routes
}

/**
 * UI state for the NearbyBusStopsSheet screen.
 *
 * @param busStopsWithArrivals List of bus stops near the map center, each with their routes & arrivals
 * @param isLoadingArrivals Whether ANY stop is still loading arrivals
 * @param errorMessage Global error message if the entire load failed
 */
data class NearbyBusStopsUiState(
    val busStopsWithArrivals: List<NearbyBusStopWithArrivals> = emptyList(),
    val isLoadingArrivals: Boolean = false,
    val errorMessage: String? = null,
)
