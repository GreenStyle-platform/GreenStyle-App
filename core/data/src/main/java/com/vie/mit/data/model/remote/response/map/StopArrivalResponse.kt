package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Arrival information for a single vehicle/trip of a route.
 */
@Serializable
data class RouteVehicleArrivalResponse(
    @SerialName("arrivalTimeSeconds") val arrivalTimeSeconds: Int? = null,
    @SerialName("etaMinutes") val etaMinutes: Int? = null,
    @SerialName("vehicleId") val vehicleId: String? = null,
)

/**
 * API response model for a route passing through a bus stop,
 * containing a list of upcoming arrival times for that route at that stop.
 */
@Serializable
data class StopArrivalResponse(
    @SerialName("stopId") val stopId: String,
    @SerialName("routeId") val routeId: String = "",
    @SerialName("routeShortName") val routeShortName: String = "",
    @SerialName("routeLongName") val routeLongName: String = "",
    @SerialName("headsign") val headsign: String = "",
    @SerialName("arrivals") val arrivals: List<RouteVehicleArrivalResponse> = emptyList(),
    @SerialName("arrivalTimes") val arrivalTimes: List<RouteVehicleArrivalResponse> = emptyList(),
)
