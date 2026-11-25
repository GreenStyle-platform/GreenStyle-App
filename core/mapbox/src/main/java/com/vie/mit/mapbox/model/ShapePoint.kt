package com.vie.mit.mapbox.model

/**
 * Decoupled data class representing a coordinate point along a transit trip's shape.
 *
 * @param shapeId Identifier of the shape
 * @param sequence Sequence number indicating the point's order along the shape
 * @param latitude Latitude coordinate
 * @param longitude Longitude coordinate
 * @param distTraveled Distance traveled along the shape from the first point
 */
data class ShapePoint(
    val shapeId: String? = null,
    val sequence: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val distTraveled: Double? = null,
)
