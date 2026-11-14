package com.vie.mit.green.ui.home.nearbytransitsheet

import com.vie.mit.mapbox.model.LocationData

/**
 * Representing a public transit stop item for the nearby list.
 *
 * @param stopId Unique identifier of the stop
 * @param name Name of the stop
 * @param typeId Type identifier (e.g., "BUS", "METRO", "TRAIN", "BIKE")
 * @param location Coordinates of the stop
 * @param distanceMeters Distance from current reference location/map center in meters
 * @param routesOrInfo Information text such as bus routes, metro line, or available bikes
 * @param properties Raw GeoJSON properties
 */
data class NearbyStopItem(
    val stopId: String,
    val name: String,
    val typeId: String,
    val location: LocationData,
    val distanceMeters: Float,
    val routesOrInfo: String,
    val properties: Map<String, String> = emptyMap(),
)
