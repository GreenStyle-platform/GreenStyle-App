package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote API response model representing a GTFS trip of a route.
 */
@Serializable
data class RouteTripResponse(
    @SerialName("tripId") val tripId: String,
    @SerialName("tripHeadsign") val tripHeadsign: String? = null,
    @SerialName("directionId") val directionId: Int? = null,
    @SerialName("serviceId") val serviceId: String? = null,
    @SerialName("shapeId") val shapeId: String? = null,
    @SerialName("routeId") val routeId: String? = null,
    @SerialName("routeShortName") val routeShortName: String? = null,
    @SerialName("routeLongName") val routeLongName: String? = null,
    @SerialName("routeType") val routeType: Int? = null,
)
