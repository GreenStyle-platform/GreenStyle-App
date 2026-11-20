package com.vie.mit.green.ui.transitstopdetail

import com.vie.mit.data.model.domain.map.StopArrival

/**
 * UI state for the Transit Stop Detail screen.
 */
data class TransitStopDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val stopId: String = "",
    val stopName: String = "",
    val typeId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
    val routeArrivals: List<StopArrival> = emptyList(),
)
