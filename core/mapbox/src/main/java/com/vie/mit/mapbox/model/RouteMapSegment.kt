package com.vie.mit.mapbox.model

/**
 * A single polyline leg to render on [com.vie.mit.mapbox.composable.RoutePickerMap].
 */
data class RouteMapSegment(
    val points: List<LocationData>,
    val colorHex: String,
    val isDashed: Boolean = false,
)

/**
 * Boarding, alighting, or transfer stop marker along a rendered route option.
 */
data class RouteMapStopPoint(
    val name: String,
    val location: LocationData,
    val colorHex: String,
)
