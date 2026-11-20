package com.vie.mit.data.model.remote.request.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransitStopBoundsRequest(
    @SerialName("minLat") val minLat: Double,
    @SerialName("minLon") val minLon: Double,
    @SerialName("maxLat") val maxLat: Double,
    @SerialName("maxLon") val maxLon: Double
)
