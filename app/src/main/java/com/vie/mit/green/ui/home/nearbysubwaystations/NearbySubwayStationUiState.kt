package com.vie.mit.green.ui.home.nearbysubwaystations

import com.vie.mit.data.model.domain.map.BusRoute

/**
 * Tab mode for NearbySubwayStationsSheet:
 * - [STATIONS]: Displays nearby subway/metro stations.
 * - [ROUTES]: Displays metro routes list fetched from backend.
 */
enum class SubwaySheetTab {
    STATIONS,
    ROUTES,
}

/**
 * UI State for NearbySubwayStationsSheet.
 */
data class NearbySubwayStationUiState(
    val selectedTab: SubwaySheetTab = SubwaySheetTab.STATIONS,
    val subwayRoutes: List<BusRoute> = emptyList(),
    val isLoadingRoutes: Boolean = false,
    val routesErrorMessage: String? = null,
)
