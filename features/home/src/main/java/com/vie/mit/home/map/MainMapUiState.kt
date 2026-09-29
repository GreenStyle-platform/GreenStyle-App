package com.vie.mit.home.map

import com.vie.mit.data.network.model.map.PublicTransportSuggestion
import com.vie.mit.mapbox.model.LocationData

data class MainMapUiState(
    val isShowBottomSheet: Boolean = false,
    val currentLocation: LocationData? = null,
    val publicTransportSuggestions: List<PublicTransportSuggestion> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val suggestionsError: String? = null,
    val searchQuery: String = "",
)
