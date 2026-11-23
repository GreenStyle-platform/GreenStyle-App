@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.vie.mit.mapbox.composable

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.location
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.mapbox.model.ShapePoint
import com.vie.mit.mapbox.model.TransitStopMarker
import com.vie.mit.mapbox.permission.MapPermissionHandler
import com.vie.mit.mapbox.util.createZoomScaled2DPuck
import kotlin.math.abs

private const val ROUTE_PATH_SOURCE_ID = "source_transit_route_path"
private const val ROUTE_PATH_LAYER_ID = "layer_transit_route_path"
private const val ROUTE_STOPS_SOURCE_ID = "source_transit_route_stops"
private const val ROUTE_STOPS_OUTER_LAYER_ID = "layer_transit_route_stops_outer"
private const val ROUTE_STOPS_INNER_LAYER_ID = "layer_transit_route_stops_inner"

/**
 * Reusable Map composable that renders:
 * 1. A polyline route path connecting trip shape coordinates.
 * 2. Visual station/stop markers for transit stops along the route.
 * 3. Automatically fits the camera viewport around the route path and stops.
 */
@Composable
fun TransitRouteMap(
    shapes: List<ShapePoint>,
    stops: List<TransitStopMarker>,
    modifier: Modifier = Modifier,
    routeColorHex: String? = null,
    cameraBottomPadding: Float = 0f,
    showUserLocation: Boolean = true,
    mapViewportState: MapViewportState = rememberMapViewportState(),
) {
    val context = LocalContext.current
    val busColor = AppTheme.colors.busColor

    val isPermissionGrantedInitially = remember(context) {
        (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED)
    }
    var showMap by remember {
        mutableStateOf(if (showUserLocation) isPermissionGrantedInitially else true)
    }

    if (showUserLocation) {
        MapPermissionHandler(
            onPermissionGranted = { showMap = true },
            onPermissionDenied = { showMap = false },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showMap) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
            ) {
                // 1. Draw Path (LineLayer) from Shape Points
                MapEffect(shapes, routeColorHex) { mapView ->
                    val mapboxMap = mapView.mapboxMap
                    mapboxMap.getStyle { style ->
                        val points = shapes.map { Point.fromLngLat(it.longitude, it.latitude) }
                        val pathGeoJson = if (points.size >= 2) {
                            val lineString = LineString.fromLngLats(points)
                            FeatureCollection.fromFeature(Feature.fromGeometry(lineString)).toJson()
                        } else {
                            FeatureCollection.fromFeatures(emptyList()).toJson()
                        }

                        val existingSource = style.getSourceAs<GeoJsonSource>(ROUTE_PATH_SOURCE_ID)
                        if (existingSource != null) {
                            existingSource.data(pathGeoJson)
                        } else {
                            val source = geoJsonSource(ROUTE_PATH_SOURCE_ID) {
                                data(pathGeoJson)
                            }
                            style.addSource(source)
                        }

                        val parsedColor = try {
                            if (!routeColorHex.isNullOrBlank()) {
                                android.graphics.Color.parseColor(
                                    if (routeColorHex.startsWith("#")) routeColorHex else "#$routeColorHex"
                                )
                            } else {
                                busColor.toArgb()
                            }
                        } catch (_: Exception) {
                            busColor.toArgb()
                        }

                        if (!style.styleLayerExists(ROUTE_PATH_LAYER_ID)) {
                            val layer = lineLayer(ROUTE_PATH_LAYER_ID, ROUTE_PATH_SOURCE_ID) {
                                lineColor(parsedColor)
                                lineWidth(5.0)
                                lineCap(LineCap.ROUND)
                                lineJoin(LineJoin.ROUND)
                            }
                            style.addLayer(layer)
                        }
                    }
                }

                // 2. Draw Stops (CircleLayer) along the trip
                MapEffect(stops, routeColorHex) { mapView ->
                    val mapboxMap = mapView.mapboxMap
                    mapboxMap.getStyle { style ->
                        val features = stops.mapIndexed { index, stop ->
                            val feat = Feature.fromGeometry(Point.fromLngLat(stop.longitude, stop.latitude))
                            feat.addStringProperty("id", stop.id)
                            feat.addStringProperty("name", stop.name)
                            feat.addNumberProperty("sequence", index + 1)
                            feat
                        }
                        val stopsGeoJson = FeatureCollection.fromFeatures(features).toJson()

                        val existingSource = style.getSourceAs<GeoJsonSource>(ROUTE_STOPS_SOURCE_ID)
                        if (existingSource != null) {
                            existingSource.data(stopsGeoJson)
                        } else {
                            val source = geoJsonSource(ROUTE_STOPS_SOURCE_ID) {
                                data(stopsGeoJson)
                            }
                            style.addSource(source)
                        }

                        val parsedColor = try {
                            if (!routeColorHex.isNullOrBlank()) {
                                android.graphics.Color.parseColor(
                                    if (routeColorHex.startsWith("#")) routeColorHex else "#$routeColorHex"
                                )
                            } else {
                                busColor.toArgb()
                            }
                        } catch (_: Exception) {
                            busColor.toArgb()
                        }

                        // Outer white ring
                        if (!style.styleLayerExists(ROUTE_STOPS_OUTER_LAYER_ID)) {
                            val outerLayer = circleLayer(ROUTE_STOPS_OUTER_LAYER_ID, ROUTE_STOPS_SOURCE_ID) {
                                circleRadius(6.5)
                                circleColor(android.graphics.Color.WHITE)
                                circleStrokeWidth(1.0)
                                circleStrokeColor(android.graphics.Color.LTGRAY)
                            }
                            style.addLayer(outerLayer)
                        }

                        // Inner colored circle
                        if (!style.styleLayerExists(ROUTE_STOPS_INNER_LAYER_ID)) {
                            val innerLayer = circleLayer(ROUTE_STOPS_INNER_LAYER_ID, ROUTE_STOPS_SOURCE_ID) {
                                circleRadius(4.0)
                                circleColor(parsedColor)
                            }
                            style.addLayer(innerLayer)
                        }
                    }
                }

                // 3. Focus Camera to bounds of shapes/stops
                MapEffect(shapes, stops, cameraBottomPadding) { mapView ->
                    val mapboxMap = mapView.mapboxMap
                    val shapePoints = shapes.map { Point.fromLngLat(it.longitude, it.latitude) }
                    val stopPoints = stops.map { Point.fromLngLat(it.longitude, it.latitude) }
                    val allPoints = if (shapePoints.isNotEmpty()) shapePoints else stopPoints

                    if (allPoints.isNotEmpty()) {
                        val bottomPad = (cameraBottomPadding + 80f).toDouble().coerceAtLeast(80.0)
                        val insets = EdgeInsets(100.0, 60.0, bottomPad, 60.0)
                        val camera = mapboxMap.cameraForCoordinates(
                            allPoints,
                            cameraOptions { },
                            insets,
                            null,
                            null,
                        )
                        mapboxMap.setCamera(camera)
                    }
                }

                // 4. Sync camera bottom padding with BottomSheet movement
                MapEffect(cameraBottomPadding) { mapView ->
                    val mapboxMap = mapView.mapboxMap
                    val currentCamera = mapboxMap.cameraState
                    val targetBottom = cameraBottomPadding.toDouble().coerceAtLeast(0.0)
                    if (abs(currentCamera.padding.bottom - targetBottom) >= 2.0) {
                        mapboxMap.setCamera(
                            cameraOptions {
                                center(currentCamera.center)
                                zoom(currentCamera.zoom)
                                pitch(currentCamera.pitch)
                                bearing(currentCamera.bearing)
                                padding(EdgeInsets(0.0, 0.0, targetBottom, 0.0))
                            },
                        )
                    }
                }

                // 5. User location puck
                if (showUserLocation) {
                    MapEffect(Unit) { mapView ->
                        val locationPlugin = mapView.location
                        locationPlugin.updateSettings {
                            locationPuck = createZoomScaled2DPuck(withBearing = true)
                            enabled = true
                            puckBearing = PuckBearing.HEADING
                            puckBearingEnabled = true
                            showAccuracyRing = true
                        }
                    }
                }
            }
        }
    }
}
