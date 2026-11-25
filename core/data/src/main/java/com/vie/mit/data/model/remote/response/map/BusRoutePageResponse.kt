package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Paginated API response wrapper for bus route search.
 */
@Serializable
data class BusRoutePageResponse(
    @SerialName("content") val content: List<BusRouteResponse> = emptyList(),
    @SerialName("currentPage") val currentPage: Int = 1,
    @SerialName("first") val first: Boolean = true,
    @SerialName("last") val last: Boolean = true,
    @SerialName("limit") val limit: Int = 20,
    @SerialName("totalElements") val totalElements: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
)
