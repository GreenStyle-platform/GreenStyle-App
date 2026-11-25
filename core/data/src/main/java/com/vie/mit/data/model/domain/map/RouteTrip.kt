package com.vie.mit.data.model.domain.map

/**
 * Domain model representing a GTFS trip of a transit route.
 *
 * @param tripId Unique identifier of the trip
 * @param tripHeadsign Text that identifies the trip's destination to passengers
 * @param directionId Indicates direction of travel: 0 for one direction (e.g. outbound), 1 for opposite (e.g. inbound)
 * @param serviceId Uniquely identifies a set of dates when service is available
 * @param shapeId Optional identifier of the shape associated with this trip
 * @param routeId Identifier of the route associated with this trip
 * @param routeShortName Short display name of the route, e.g. "08B"
 * @param routeLongName Full name of the route, e.g. "Long Biên ↔ Vạn Phúc"
 * @param routeType Type of transit (e.g. 3 for Bus)
 */
data class RouteTrip(
    val tripId: String,
    val tripHeadsign: String,
    val directionId: Int,
    val serviceId: String = "",
    val shapeId: String? = null,
    val routeId: String = "",
    val routeShortName: String = "",
    val routeLongName: String = "",
    val routeType: Int = 3,
)
