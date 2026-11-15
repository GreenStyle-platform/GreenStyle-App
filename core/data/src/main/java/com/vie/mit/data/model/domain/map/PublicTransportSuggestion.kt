package com.vie.mit.data.model.domain.map

import com.vie.mit.data.model.common.map.PublicTransportType

data class PublicTransportSuggestion(
    val name: String,
    val type: PublicTransportType,
    val distance: Long,
    val availableIn: Int,
)
