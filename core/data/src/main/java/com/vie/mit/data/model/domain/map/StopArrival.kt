package com.vie.mit.data.model.domain.map

/**
 * Domain model for an arrival time of a vehicle on a route.
 *
 * @param etaMinutes Minutes until the bus arrives (rounded)
 * @param vehicleId Optional vehicle identifier
 */
data class VehicleArrival(
    val etaMinutes: Int,
    val vehicleId: String? = null,
)

/**
 * Domain model for a bus route arrival at a specific stop.
 *
 * @param stopId Identifier of the bus stop
 * @param routeId Unique identifier of the route
 * @param routeShortName Short display name, e.g. "36"
 * @param routeLongName Full route name, e.g. "Bến Thành - Chợ Lớn"
 * @param headsign Final destination displayed on the bus
 * @param arrivals List of upcoming arrival times for this route at this stop, sorted by ETA ascending
 */
data class StopArrival(
    val stopId: String,
    val routeId: String,
    val routeShortName: String,
    val routeLongName: String,
    val headsign: String,
    val arrivals: List<VehicleArrival> = emptyList(),
)
