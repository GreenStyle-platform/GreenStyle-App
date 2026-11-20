package com.vie.mit.data.model.domain.map

/**
 * Domain model for an arrival time of a vehicle on a route.
 *
 * @param etaMinutes Minutes until the bus arrives (rounded)
 * @param arrivalTime Formatted scheduled arrival time (e.g. "05:18")
 * @param vehicleId Optional vehicle identifier
 */
data class VehicleArrival(
    val etaMinutes: Int,
    val arrivalTime: String? = null,
    val vehicleId: String? = null,
)

/**
 * Domain model for a transit route arrival at a specific stop.
 *
 * @param stopId Identifier of the transit stop
 * @param routeId Unique identifier of the route
 * @param routeShortName Short display name, e.g. "36"
 * @param routeLongName Full route name, e.g. "Bến Thành - Chợ Lớn"
 * @param headsign Final destination displayed on the bus
 * @param routeColor Optional hex color string for the route (e.g. "#FF0000")
 * @param routeTextColor Optional hex color string for the route text
 * @param arrivals List of upcoming arrival times for this route at this stop, sorted by ETA ascending
 */
data class StopArrival(
    val stopId: String,
    val routeId: String,
    val routeShortName: String,
    val routeLongName: String,
    val headsign: String = "",
    val routeColor: String? = null,
    val routeTextColor: String? = null,
    val arrivals: List<VehicleArrival> = emptyList(),
)
