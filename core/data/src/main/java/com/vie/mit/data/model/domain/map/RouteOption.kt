package com.vie.mit.data.model.domain.map

enum class RouteOptionTransportType {
    BUS,
    SUBWAY,
    BIKE,
    WALK,
}

/**
 * Domain model representing a multimodal travel option between Start and End points.
 */
data class RouteOption(
    val id: String,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val costVnd: Int,
    val walkDistanceMeters: Int,
    val transportType: RouteOptionTransportType,
    val tags: List<String> = emptyList(),
)
