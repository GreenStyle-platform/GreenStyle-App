package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote API response model representing a single coordinate point of a transit trip's shape.
 */
@Serializable
data class ShapePointResponse(
    @SerialName("shapeId") val shapeId: String? = null,
    @SerialName("shapePtSequence") val shapePtSequence: Int? = null,
    @SerialName("shapePtLat") val shapePtLat: Double? = null,
    @SerialName("shapePtLon") val shapePtLon: Double? = null,
    @SerialName("shapeDistTraveled") val shapeDistTraveled: Double? = null,
)
