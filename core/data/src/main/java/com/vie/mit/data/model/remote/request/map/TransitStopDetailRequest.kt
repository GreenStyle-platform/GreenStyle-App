package com.vie.mit.data.model.remote.request.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for fetching detailed information about a transit stop.
 */
@Serializable
data class TransitStopDetailRequest(
    @SerialName("stopId") val stopId: String,
)
