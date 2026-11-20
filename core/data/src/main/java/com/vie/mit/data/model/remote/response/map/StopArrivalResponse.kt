package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stop time response representing a vehicle scheduled arrival/departure at a stop.
 */
@Serializable
data class StopTimeResponse(
    @SerialName("arrivalTime") val arrivalTime: String,
    @SerialName("departureTime") val departureTime: String? = null,
    @SerialName("timepoint") val timepoint: Int? = null,
)

/**
 * Route response representing a transit route passing through a stop,
 * containing its scheduled stop times.
 */
@Serializable
data class RouteArrivalResponse(
    @SerialName("routeId") val routeId: String,
    @SerialName("routeShortName") val routeShortName: String? = null,
    @SerialName("routeLongName") val routeLongName: String? = null,
    @SerialName("routeDesc") val routeDesc: String? = null,
    @SerialName("routeType") val routeType: Int? = null,
    @SerialName("routeUrl") val routeUrl: String? = null,
    @SerialName("routeColor") val routeColor: String? = null,
    @SerialName("routeTextColor") val routeTextColor: String? = null,
    @SerialName("stopTimeResponses") val stopTimeResponses: List<StopTimeResponse> = emptyList(),
)
