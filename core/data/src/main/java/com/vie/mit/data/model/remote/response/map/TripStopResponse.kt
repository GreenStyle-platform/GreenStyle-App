package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote API response model representing a transit stop within a specific trip.
 */
@Serializable
data class TripStopResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
)
