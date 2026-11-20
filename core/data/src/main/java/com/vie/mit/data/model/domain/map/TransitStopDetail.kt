package com.vie.mit.data.model.domain.map

/**
 * Domain model representing detailed information about a transit stop.
 *
 * @param id Unique identifier of the stop
 * @param name Display name of the stop
 * @param latitude Geographic latitude
 * @param longitude Geographic longitude
 * @param address Optional street address
 * @param type Transit type (BUS, SUBWAY, etc.)
 * @param routeArrivals List of routes passing through this stop with arrival info
 */
data class TransitStopDetail(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val type: String? = null,
    val routeArrivals: List<StopArrival> = emptyList(),
)
