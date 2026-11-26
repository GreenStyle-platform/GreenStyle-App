package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload from POST transit-routes/search backed by OpenTripPlanner 2.
 */
@Serializable
data class TransitRouteSearchResponse(
    @SerialName("itineraries") val itineraries: List<TransitItineraryResponse> = emptyList(),
)

@Serializable
data class TransitItineraryResponse(
    @SerialName("id") val id: String = "",
    @SerialName("durationSeconds") val durationSeconds: Int = 0,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null,
    @SerialName("transfers") val transfers: Int = 0,
    @SerialName("walkSeconds") val walkSeconds: Int = 0,
    @SerialName("legs") val legs: List<TransitLegResponse> = emptyList(),
)

@Serializable
data class TransitLegResponse(
    @SerialName("mode") val mode: String = "",
    @SerialName("durationSeconds") val durationSeconds: Int = 0,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null,
    @SerialName("from") val from: TransitPlaceResponse? = null,
    @SerialName("to") val to: TransitPlaceResponse? = null,
    @SerialName("geometry") val geometry: String? = null,
    @SerialName("headsign") val headsign: String? = null,
    @SerialName("routeId") val routeId: String? = null,
    @SerialName("routeShortName") val routeShortName: String? = null,
    @SerialName("routeLongName") val routeLongName: String? = null,
)

@Serializable
data class TransitPlaceResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("latitude") val latitude: Double = 0.0,
    @SerialName("longitude") val longitude: Double = 0.0,
    @SerialName("arrivalTime") val arrivalTime: String? = null,
    @SerialName("departureTime") val departureTime: String? = null,
)
