package com.vie.mit.data.model.domain.map

import com.vie.mit.data.model.common.map.TransitMode

data class PublicTransportSuggestion(
    val name: String,
    val type: TransitMode,
    val distance: Long,
    val availableIn: Int,
)
