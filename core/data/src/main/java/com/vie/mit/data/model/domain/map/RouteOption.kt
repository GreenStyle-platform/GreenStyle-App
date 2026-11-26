package com.vie.mit.data.model.domain.map

import com.vie.mit.data.model.common.map.TransitMode

/**
 * Geographic coordinate point decoded from a route leg geometry.
 */
data class RouteCoordinate(
    val latitude: Double,
    val longitude: Double,
)

/**
 * Origin or destination stop/point of a single route leg.
 */
data class RouteLegPlace(
    val id: String? = null,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val arrivalTimeMillis: Long? = null,
    val departureTimeMillis: Long? = null,
)

/**
 * A single leg inside a multimodal transit itinerary (e.g. WALK, BUS 29, SUBWAY 2A).
 */
data class RouteLeg(
    val mode: TransitMode,
    val durationSeconds: Int,
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
    val from: RouteLegPlace? = null,
    val to: RouteLegPlace? = null,
    val encodedGeometry: String? = null,
    val points: List<RouteCoordinate> = emptyList(),
    val headsign: String? = null,
    val routeId: String? = null,
    val routeShortName: String? = null,
    val routeLongName: String? = null,
) {
    val durationMinutes: Int
        get() = if (durationSeconds <= 0) 0 else ((durationSeconds + 30) / 60).coerceAtLeast(1)
}

/**
 * Domain model representing a multimodal travel itinerary between Start and End points.
 */
data class RouteOption(
    val id: String,
    val title: String,
    val description: String,
    val durationSeconds: Int,
    val durationMinutes: Int,
    val walkSeconds: Int = 0,
    val transfers: Int = 0,
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
    val costVnd: Int? = null,
    val transportType: TransitMode,
    val transportTypes: Set<TransitMode> = setOf(transportType),
    val legs: List<RouteLeg> = emptyList(),
    val tags: List<String> = emptyList(),
) {
    val walkMinutes: Int
        get() = if (walkSeconds <= 0) 0 else ((walkSeconds + 30) / 60).coerceAtLeast(1)
}
