package com.vie.mit.data.network.model.map

import kotlinx.serialization.Serializable

@Serializable
data class TransitStopBoundsRequest(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
)
