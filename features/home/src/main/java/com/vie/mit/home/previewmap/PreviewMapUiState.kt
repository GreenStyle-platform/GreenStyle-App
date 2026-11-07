package com.vie.mit.home.previewmap

import com.vie.mit.data.network.model.map.PublicTransportSuggestion
import com.vie.mit.mapbox.model.LocationData

data class PreviewMapUiState(
    val isShowBottomSheet: Boolean = false,
    val currentLocation: LocationData? = null,
    val publicTransportSuggestions: List<PublicTransportSuggestion> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val suggestionsError: String? = null,
    val searchQuery: String = "",
)
