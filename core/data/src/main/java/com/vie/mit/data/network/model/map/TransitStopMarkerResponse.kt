package com.vie.mit.data.network.model.map

import kotlinx.serialization.Serializable

@Serializable
data class TransitStopMarkerResponse(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)
