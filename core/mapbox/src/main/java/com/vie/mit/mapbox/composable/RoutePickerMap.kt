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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraChangedCallback
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.permission.MapPermissionHandler

private const val PICKER_LINE_SOURCE_ID = "source_route_picker_dashed_line"
private const val PICKER_LINE_LAYER_ID = "layer_route_picker_dashed_line"
private const val PICKER_POINTS_SOURCE_ID = "source_route_picker_points"
private const val PICKER_POINTS_OUTER_LAYER_ID = "layer_route_picker_points_outer"
private const val PICKER_POINTS_INNER_LAYER_ID = "layer_route_picker_points_inner"

/**
 * Composable Mapbox hiển thị cho màn hình RouteLocationPickerScreen:
 * - Kèm Center Location Pin ở tâm màn hình để người dùng kéo chọn vị trí trực tiếp.
 * - Khi focus Start: hiển thị Icon Start (Xanh lá).
 * - Khi focus End: hiển thị Icon End (Đỏ).
 * - Khi cả 2 điểm start & end được chọn: vẽ 1 đường nét đứt (dashed polyline) nối từ start đến end và cắm 2 marker cố định.
 */
@OptIn(MapboxDelicateApi::class)
@Composable
fun RoutePickerMap(
    startPoint: LocationData?,
    endPoint: LocationData?,
    onCameraCenterChanged: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
    cameraBottomPadding: Float = 0f,
    showUserLocation: Boolean = true,
    mapViewportState: MapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(106.7009, 10.7769))
            zoom(14.0)
            pitch(0.0)
        }
    },
    onLocationUpdate: ((LocationData) -> Unit)? = null,
    centerOverlay: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val primaryColor = AppTheme.colors.navyBlue
    val currentOnLocationUpdate by rememberUpdatedState(onLocationUpdate)

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
                // 1. Lắng nghe sự kiện di chuyển camera để lấy toạ độ tâm map
                MapEffect(Unit) { mapView ->
                    val mapboxMap = mapView.mapboxMap
                    val cameraCallback = CameraChangedCallback {
                        val center = mapboxMap.cameraState.center
                        onCameraCenterChanged(center.latitude(), center.longitude())
                    }
                    val cancellable = mapboxMap.subscribeCameraChanged(cameraCallback)

                    mapView.location.updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }

                    // Lắng nghe vị trí người dùng và gọi callback
                    val indicatorListener = OnIndicatorPositionChangedListener { point ->
                        val loc = LocationData(
                            latitude = point.latitude(),
                            longitude = point.longitude(),
                        )
                        currentOnLocationUpdate?.invoke(loc)
                    }
                    mapView.location.addOnIndicatorPositionChangedListener(indicatorListener)
                }

                // 2. Vẽ đường nét đứt (Dashed Polyline) nối giữa start và end
                MapEffect(startPoint, endPoint) { mapView ->
                    val mapboxMap = mapView.mapboxMap
                    mapboxMap.getStyle { style ->
                        val hasBothPoints = startPoint != null && endPoint != null
                        val lineGeoJson = if (hasBothPoints) {
                            val coordinates = listOf(
                                Point.fromLngLat(startPoint.longitude, startPoint.latitude),
                                Point.fromLngLat(endPoint.longitude, endPoint.latitude),
                            )
                            val lineString = LineString.fromLngLats(coordinates)
                            FeatureCollection.fromFeature(Feature.fromGeometry(lineString)).toJson()
                        } else {
                            FeatureCollection.fromFeatures(emptyList()).toJson()
                        }

                        val existingSource = style.getSourceAs<GeoJsonSource>(PICKER_LINE_SOURCE_ID)
                        if (existingSource != null) {
                            existingSource.data(lineGeoJson)
                        } else {
                            val source = geoJsonSource(PICKER_LINE_SOURCE_ID) {
                                data(lineGeoJson)
                            }
                            style.addSource(source)
                        }

                        if (!style.styleLayerExists(PICKER_LINE_LAYER_ID)) {
                            val layer = lineLayer(PICKER_LINE_LAYER_ID, PICKER_LINE_SOURCE_ID) {
                                lineColor(primaryColor.toArgb())
                                lineWidth(4.5)
                                lineCap(LineCap.ROUND)
                                lineJoin(LineJoin.ROUND)
                                // Thuộc tính vẽ nét đứt
                                lineDasharray(listOf(2.0, 2.0))
                            }
                            style.addLayer(layer)
                        }

                        // 3. Hiển thị 2 Marker cố định tại Start và End
                        val pointFeatures = mutableListOf<Feature>()
                        if (startPoint != null) {
                            val feat = Feature.fromGeometry(
                                Point.fromLngLat(startPoint.longitude, startPoint.latitude)
                            )
                            feat.addStringProperty("point_type", "start")
                            pointFeatures.add(feat)
                        }
                        if (endPoint != null) {
                            val feat = Feature.fromGeometry(
                                Point.fromLngLat(endPoint.longitude, endPoint.latitude)
                            )
                            feat.addStringProperty("point_type", "end")
                            pointFeatures.add(feat)
                        }

                        val pointsGeoJson = FeatureCollection.fromFeatures(pointFeatures).toJson()
                        val existingPointsSource = style.getSourceAs<GeoJsonSource>(PICKER_POINTS_SOURCE_ID)
                        if (existingPointsSource != null) {
                            existingPointsSource.data(pointsGeoJson)
                        } else {
                            val source = geoJsonSource(PICKER_POINTS_SOURCE_ID) {
                                data(pointsGeoJson)
                            }
                            style.addSource(source)
                        }

                        // Vòng ngoài marker (Xanh lá cho start, Đỏ cho end)
                        if (!style.styleLayerExists(PICKER_POINTS_OUTER_LAYER_ID)) {
                            val outerLayer = circleLayer(PICKER_POINTS_OUTER_LAYER_ID, PICKER_POINTS_SOURCE_ID) {
                                circleRadius(11.0)
                                circleColor(
                                    match {
                                        get { literal("point_type") }
                                        stop {
                                            literal("start")
                                            literal("#10B981") // Emerald Green
                                        }
                                        stop {
                                            literal("end")
                                            literal("#EF4444") // Red
                                        }
                                        literal("#3B82F6")
                                    }
                                )
                                circleStrokeWidth(3.0)
                                circleStrokeColor("#FFFFFF")
                            }
                            style.addLayer(outerLayer)
                        }

                        // Vòng tròn trắng bên trong marker
                        if (!style.styleLayerExists(PICKER_POINTS_INNER_LAYER_ID)) {
                            val innerLayer = circleLayer(PICKER_POINTS_INNER_LAYER_ID, PICKER_POINTS_SOURCE_ID) {
                                circleRadius(4.5)
                                circleColor("#FFFFFF")
                            }
                            style.addLayer(innerLayer)
                        }

                        // 4. Khi có cả 2 điểm, tự động zoom bao trọn cả 2 vị trí
                        if (hasBothPoints) {
                            val points = listOf(
                                Point.fromLngLat(startPoint.longitude, startPoint.latitude),
                                Point.fromLngLat(endPoint.longitude, endPoint.latitude),
                            )
                            val camera = mapboxMap.cameraForCoordinates(
                                points,
                                EdgeInsets(
                                    140.0,
                                    100.0,
                                    (cameraBottomPadding.toDouble() + 120.0).coerceAtLeast(140.0),
                                    100.0
                                ),
                                null,
                                null,
                            )
                            mapboxMap.setCamera(camera)
                        }
                    }
                }
            }
        }

        // Center overlay (e.g. Center Location Pin)
        if (centerOverlay != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                centerOverlay()
            }
        }
    }
}
