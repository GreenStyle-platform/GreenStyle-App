package com.vie.mit.data.model.remote.request.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for searching OpenTripPlanner 2 transit route itineraries.
 *
 * Endpoint: POST transit-routes/search
 */
@Serializable
data class TransitRouteSearchRequest(
    @SerialName("originLatitude") val originLatitude: Double,
    @SerialName("originLongitude") val originLongitude: Double,
    @SerialName("destinationLatitude") val destinationLatitude: Double,
    @SerialName("destinationLongitude") val destinationLongitude: Double,
    @SerialName("departureTime") val departureTime: String,
    @SerialName("maxItineraries") val maxItineraries: Int,
)
