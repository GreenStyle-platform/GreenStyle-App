package com.vie.mit.data.model.domain.map

/**
 * Domain model representing a bus route in the app.
 *
 * @param routeId Unique identifier of the route
 * @param routeShortName Short display name, e.g. "01", "36"
 * @param routeLongName Full route name, e.g. "Bến Thành - Chợ Lớn"
 * @param routeDesc Additional description or trajectory overview
 * @param routeColor Optional hex color string for the route
 * @param routeTextColor Optional hex color string for text on route background
 */
data class BusRoute(
    val routeId: String,
    val routeShortName: String,
    val routeLongName: String,
    val routeDesc: String = "",
    val routeColor: String? = null,
    val routeTextColor: String? = null,
)
