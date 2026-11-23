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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.locationcomponent.location
import com.vie.mit.mapbox.model.TransitTypeConfig
import com.vie.mit.mapbox.permission.MapPermissionHandler
import com.vie.mit.mapbox.util.bitmapFromDrawableRes
import com.vie.mit.mapbox.util.createZoomScaled2DPuck
import kotlin.math.abs

private const val FOCUS_STOP_SOURCE_ID = "source_focus_stop"
private const val FOCUS_STOP_LAYER_ID = "layer_focus_stop"

/**
 * Simplified Mapbox composable that focuses the camera on a single transit stop
 * and displays its marker icon. Designed for the Transit Stop Detail screen.
 *
 * Unlike [TransitStopsMapScreen], this composable:
 * - Shows only a single stop marker (no clustering, no multiple sources).
 * - Automatically flies the camera to the stop's coordinates.
 * - Syncs camera bottom padding with the BottomSheet.
 * - Does not handle map click/gesture events.
 *
 * @param stopLatitude Latitude of the transit stop.
 * @param stopLongitude Longitude of the transit stop.
 * @param stopName Display name of the stop (shown as label).
 * @param modifier Modifier for the map container.
 * @param transitTypeConfig Visual configuration for the stop marker icon. If null, uses a default pin.
 * @param showUserLocation Whether to display the user's location puck.
 * @param cameraBottomPadding Camera bottom padding in pixels (synced with BottomSheet).
 * @param zoom Camera zoom level for focusing on the stop.
 * @param mapViewportState Optional externally-controlled map viewport state.
 */
@Composable
fun TransitStopFocusMap(
    stopLatitude: Double,
    stopLongitude: Double,
    stopName: String,
    modifier: Modifier = Modifier,
    transitTypeConfig: TransitTypeConfig? = null,
    showUserLocation: Boolean = true,
    cameraBottomPadding: Float = 0f,
    zoom: Double = 16.0,
    mapViewportState: MapViewportState = rememberMapViewportState(),
) {
    val context = LocalContext.current

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
                // 1. Fly camera to stop location
                MapEffect(stopLatitude, stopLongitude, zoom) { _ ->
                    if (stopLatitude != 0.0 || stopLongitude != 0.0) {
                        val point = Point.fromLngLat(stopLongitude, stopLatitude)
                        mapViewportState.flyTo(
                            cameraOptions {
                                center(point)
                                zoom(zoom)
                                pitch(0.0)
                            },
                            MapAnimationOptions.mapAnimationOptions { duration(900) },
                        )
                    }
                }

                // 2. Sync camera bottom padding with BottomSheet
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

                // 3. Add single stop marker icon and GeoJSON source
                MapEffect(stopLatitude, stopLongitude, stopName, transitTypeConfig) { mapView ->
                    val mapboxMap = mapView.mapboxMap
                    mapboxMap.getStyle { style ->
                        // Register transit icon if config provided, otherwise fallback to default pin
                        if (transitTypeConfig != null) {
                            if (style.getStyleImage(transitTypeConfig.iconName) == null) {
                                val bitmap = bitmapFromDrawableRes(context, transitTypeConfig.iconResId)
                                if (bitmap != null) {
                                    style.addImage(transitTypeConfig.iconName, bitmap)
                                }
                            }
                        } else {
                            if (style.getStyleImage("transit_icon_default") == null) {
                                val bitmap = bitmapFromDrawableRes(context, com.vie.mit.mapbox.R.drawable.ic_pin_map)
                                if (bitmap != null) {
                                    style.addImage("transit_icon_default", bitmap)
                                }
                            }
                        }

                        // Build single-point GeoJSON FeatureCollection
                        val feature = Feature.fromGeometry(Point.fromLngLat(stopLongitude, stopLatitude))
                        feature.addStringProperty("name", stopName)
                        val geoJson = FeatureCollection.fromFeature(feature).toJson()

                        // Add or update GeoJSON source
                        val existingSource =
                            style.getSourceAs<GeoJsonSource>(FOCUS_STOP_SOURCE_ID)
                        if (existingSource != null) {
                            existingSource.data(geoJson)
                        } else {
                            val source = geoJsonSource(FOCUS_STOP_SOURCE_ID) {
                                data(geoJson)
                            }
                            style.addSource(source)
                        }

                        // Add symbol layer if not exists
                        if (!style.styleLayerExists(FOCUS_STOP_LAYER_ID)) {
                            val iconName =
                                transitTypeConfig?.iconName ?: "transit_icon_default"
                            val iconSizeVal = transitTypeConfig?.iconSize ?: 1.2
                            val showStopName = transitTypeConfig?.showStopName ?: true
                            val textMinZoom = transitTypeConfig?.textMinZoom ?: 15.0
                            val textSize = transitTypeConfig?.textSize ?: 11.0
                            val textColor = transitTypeConfig?.textColor ?: 0xFF0369D8.toInt()
                            val textHaloColor = transitTypeConfig?.textHaloColor ?: 0xFFFFFFFF.toInt()
                            val textHaloWidth = transitTypeConfig?.textHaloWidth ?: 1.2

                            val layer = symbolLayer(FOCUS_STOP_LAYER_ID, FOCUS_STOP_SOURCE_ID) {
                                iconImage(iconName)
                                iconSize(iconSizeVal)
                                iconAllowOverlap(true)
                                iconIgnorePlacement(true)

                                if (showStopName) {
                                    textField(Expression.fromRaw("""["get", "name"]"""))
                                    textSize(
                                        Expression.fromRaw(
                                            """["step", ["zoom"], 0.0, $textMinZoom, $textSize]"""
                                        )
                                    )
                                    textAnchor(TextAnchor.TOP)
                                    textOffset(listOf(0.0, 1.1))
                                    textColor(textColor)
                                    textHaloColor(textHaloColor)
                                    textHaloWidth(textHaloWidth)
                                    textMaxWidth(9.0)
                                    textAllowOverlap(false)
                                    textOptional(true)
                                }
                            }
                            style.addLayer(layer)
                        }
                    }
                }

                // 4. User location puck
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
