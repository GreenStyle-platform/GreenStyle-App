package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response containing detailed information about a transit stop,
 * including its basic info and routes passing through it with scheduled arrivals.
 */
@Serializable
data class TransitStopDetailResponse(
    @SerialName("stopId") val stopId: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("stopName") val stopName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("address") val address: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("routes") val routes: List<RouteArrivalResponse> = emptyList(),
) {
    val resolvedId: String
        get() = stopId ?: id.orEmpty()
    val resolvedName: String
        get() = stopName ?: name.orEmpty()
}
