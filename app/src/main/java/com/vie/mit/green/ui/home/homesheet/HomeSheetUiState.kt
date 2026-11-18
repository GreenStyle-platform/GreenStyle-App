package com.vie.mit.green.ui.home.homesheet

import com.vie.mit.data.model.common.map.PublicTransportType
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyStopItem
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.StopFeatureClick
import com.vie.mit.mapbox.model.TransitSourceData
import com.vie.mit.mapbox.model.TransitTypeConfig

data class HomeSheetUiState(
    val currentLocation: LocationData? = null,
    val selectedRadiusKm: Double = 1.0,
    val nearbyStops: List<NearbyStopItem> = emptyList(),
    val transitSources: List<TransitSourceData> = emptyList(),
    val transitConfigs: List<TransitTypeConfig> = emptyList(),
    val activeTransitTypes: Set<String> = setOf(
        PublicTransportType.BUS.name,
        PublicTransportType.SUBWAY.name,
        PublicTransportType.RAIL.name,
        TravelMode.BICYCLE_RENT.name,
    ),
    val selectedStop: StopFeatureClick? = null,
    val isLoadingTransit: Boolean = false,
    val errorMessage: String? = null,
    val isMapFocused: Boolean = false,
    /**
     * The transit type currently focused/selected by the user (e.g. "BUS").
     * null = show all types on map.
     * When non-null, the map only renders layers for this typeId,
     * and NearbyStops list is filtered to this type only.
     */
    val selectedTransitTypeId: String? = null,
)