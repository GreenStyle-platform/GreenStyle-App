package com.vie.mit.green.ui.busroutesearch

import com.vie.mit.data.model.domain.map.BusRoute

data class BusRouteUiState(
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val routes: List<BusRoute> = emptyList(),
    val errorMessage: String? = null,
)
