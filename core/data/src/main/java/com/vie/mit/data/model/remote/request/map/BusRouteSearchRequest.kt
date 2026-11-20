package com.vie.mit.data.model.remote.request.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for searching bus routes by keyword or route code.
 */
@Serializable
data class BusRouteSearchRequest(
    @SerialName("query") val query: String = "",
)
