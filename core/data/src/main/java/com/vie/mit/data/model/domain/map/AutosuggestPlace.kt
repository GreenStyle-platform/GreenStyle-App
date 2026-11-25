package com.vie.mit.data.model.domain.map

data class AutosuggestPlace(
    val id: String,
    val title: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distance: Long? = null,
    val highlights: List<Pair<Int, Int>> = emptyList(),
)
