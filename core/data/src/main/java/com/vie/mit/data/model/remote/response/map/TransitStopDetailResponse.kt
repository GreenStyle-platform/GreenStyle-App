package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response containing detailed information about a transit stop,
 * including its basic info and routes passing through it with scheduled arrivals.
 */
@Serializable
data class TransitStopDetailResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("address") val address: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("routes") val routes: List<RouteArrivalResponse> = emptyList(),
)
