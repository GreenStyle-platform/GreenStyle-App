package com.vie.mit.mapbox.model

/**
 * Visual marker data class for a transit stop along a route.
 *
 * @param id Unique identifier of the stop
 * @param name Display name of the stop
 * @param latitude Latitude coordinate
 * @param longitude Longitude coordinate
 */
data class TransitStopMarker(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)
