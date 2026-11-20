package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote API response model representing a bus route search result.
 */
@Serializable
data class BusRouteResponse(
    @SerialName("routeId") val routeId: String,
    @SerialName("routeShortName") val routeShortName: String? = null,
    @SerialName("routeLongName") val routeLongName: String? = null,
    @SerialName("routeDesc") val routeDesc: String? = null,
    @SerialName("routeColor") val routeColor: String? = null,
    @SerialName("routeTextColor") val routeTextColor: String? = null,
)
