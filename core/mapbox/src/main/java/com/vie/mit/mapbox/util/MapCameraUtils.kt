package com.vie.mit.mapbox.util

import com.mapbox.geojson.Point
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.vie.mit.mapbox.model.LocationData

/**
 * Focuses the map camera to a bounding box around the specified [location].
 * Designed to be called on initial render or when clicking "My Location".
 *
 * @param mapViewportState MapViewportState controlling the camera viewport.
 * @param location User's location.
 * @param zoom Zoom level for the bounding box center (default 14.5).
 * @param animate Whether to smoothly animate the transition.
 */
fun focusUserLocationBoundingBox(
    mapViewportState: MapViewportState,
    location: LocationData,
    zoom: Double = 14.5,
    animate: Boolean = true,
) {
    val centerPoint = Point.fromLngLat(location.longitude, location.latitude)
    val cameraOpts = cameraOptions {
        center(centerPoint)
        zoom(zoom)
        pitch(0.0)
    }
    if (animate) {
        mapViewportState.flyTo(
            cameraOpts,
            MapAnimationOptions.mapAnimationOptions { duration(900) }
        )
    } else {
        mapViewportState.setCameraOptions(cameraOpts)
    }
}

/**
 * Focuses the map camera to fit within an exact geographic coordinate bounding box around [location].
 *
 * @param mapViewportState MapViewportState controlling camera viewport.
 * @param mapboxMap The MapboxMap controller.
 * @param location Center user location.
 * @param radiusDeltaLat Delta latitude span (~1.5km = 0.015).
 * @param radiusDeltaLon Delta longitude span (~1.5km = 0.015).
 * @param edgeInsets Screen padding in pixels around the bounding box.
 * @param animate Whether to smoothly animate the transition.
 */
@Suppress("DEPRECATION")
fun focusUserLocationBoundingBox(
    mapViewportState: MapViewportState,
    mapboxMap: MapboxMap,
    location: LocationData,
    radiusDeltaLat: Double = 0.015,
    radiusDeltaLon: Double = 0.015,
    edgeInsets: EdgeInsets = EdgeInsets(100.0, 80.0, 180.0, 80.0),
    animate: Boolean = true,
) {
    val bounds = CoordinateBounds(
        Point.fromLngLat(location.longitude - radiusDeltaLon, location.latitude - radiusDeltaLat),
        Point.fromLngLat(location.longitude + radiusDeltaLon, location.latitude + radiusDeltaLat)
    )
    val cameraOpts = mapboxMap.cameraForCoordinateBounds(
        bounds,
        edgeInsets,
        null,
        null
    )
    if (animate) {
        mapViewportState.flyTo(
            cameraOpts,
            MapAnimationOptions.mapAnimationOptions { duration(900) }
        )
    } else {
        mapViewportState.setCameraOptions(cameraOpts)
    }
}
