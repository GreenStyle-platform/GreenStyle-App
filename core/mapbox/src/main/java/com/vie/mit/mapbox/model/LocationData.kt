package com.vie.mit.mapbox.model

import com.mapbox.geojson.Point

/**
 * Decoupled location data class representing geographical coordinates.
 *
 * @property latitude The latitude in degrees.
 * @property longitude The longitude in degrees.
 * @property altitude The altitude in meters (defaults to 0.0).
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
)

/**
 * Extension function to convert Mapbox [Point] to decoupled [LocationData].
 */
fun Point.toLocationData(): LocationData = LocationData(
    latitude = latitude(),
    longitude = longitude(),
    altitude = if (hasAltitude()) altitude() else 0.0,
)

/**
 * Calculates distance in meters between this [LocationData] and [other].
 */
fun LocationData.distanceTo(other: LocationData): Float {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        latitude,
        longitude,
        other.latitude,
        other.longitude,
        results,
    )
    return results[0]
}
