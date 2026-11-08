package com.vie.mit.green.ui.home

import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.StopFeatureClick
import com.vie.mit.mapbox.model.TransitSourceData
import com.vie.mit.mapbox.model.TransitTypeConfig

data class HomeUiState(
    val currentLocation: LocationData? = null,
    val transitSources: List<TransitSourceData> = emptyList(),
    val transitConfigs: List<TransitTypeConfig> = emptyList(),
    val activeTransitTypes: Set<String> = setOf("BUS", "METRO", "TRAIN", "BIKE"),
    val selectedStop: StopFeatureClick? = null,
    val isLoadingTransit: Boolean = false,
    val errorMessage: String? = null,
)
