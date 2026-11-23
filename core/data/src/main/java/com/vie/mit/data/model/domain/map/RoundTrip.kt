package com.vie.mit.data.model.domain.map

/**
 * Domain model representing a round trip pairing (outbound and inbound)
 * sharing the same service schedule ([serviceId]) in GTFS.
 *
 * @param serviceId Service schedule identifier (e.g. "Mo,Tu,We,Th", "Fr", "Sa,Su")
 * @param outboundTrip The trip for outbound direction (directionId == 0), if available
 * @param inboundTrip The trip for inbound direction (directionId == 1), if available
 * @param routeId Route identifier associated with this round trip
 * @param routeShortName Short display name of the route (e.g. "08B")
 * @param routeLongName Full name of the route (e.g. "Long Biên ↔ Vạn Phúc")
 */
data class RoundTrip(
    val serviceId: String,
    val outboundTrip: RouteTrip? = null,
    val inboundTrip: RouteTrip? = null,
    val routeId: String = "",
    val routeShortName: String = "",
    val routeLongName: String = "",
) {
    /**
     * Gets the trip for the specified direction.
     * @param directionId 0 for outbound, 1 for inbound.
     */
    fun getTrip(directionId: Int): RouteTrip? = when (directionId) {
        0 -> outboundTrip
        1 -> inboundTrip
        else -> null
    }
}
