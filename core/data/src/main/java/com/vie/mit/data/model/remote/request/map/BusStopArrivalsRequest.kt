package com.vie.mit.data.model.remote.request.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for fetching scheduled arrivals for multiple bus stops.
 */
@Serializable
data class BusStopArrivalsRequest(
    @SerialName("stopIds") val stopIds: List<String>,
)
