@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.vie.mit.mapbox.composable

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraChangedCallback
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapIdleCallback
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.dsl.cameraOptions
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
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.permission.MapPermissionHandler
import com.vie.mit.mapbox.util.MapPlaceholderBackgroundColor
import com.vie.mit.mapbox.util.createZoomScaled2DPuck
import com.vie.mit.mapbox.util.rememberTextureMapInitOptions
import kotlinx.coroutines.awaitCancellation
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PICKER_LINE_SOURCE_ID = "source_route_picker_dashed_line"
private const val PICKER_LINE_LAYER_ID = "layer_route_picker_dashed_line"
private const val PICKER_POINTS_SOURCE_ID = "source_route_picker_points"
private const val PICKER_POINTS_OUTER_LAYER_ID = "layer_route_picker_points_outer"
private const val PICKER_POINTS_INNER_LAYER_ID = "layer_route_picker_points_inner"

/**
 * Composable Mapbox hiển thị cho luồng RoutePlanner:
 * - Đồng bộ camera bottom padding mượt mà theo chiều cao BottomSheet (giống TransitStopsMapScreen).
 * - Kèm Center Location Pin căn chính xác theo tâm camera thực (offset theo cameraBottomPadding / 2).
 * - Hỗ trợ bắt sự kiện tap vào bản đồ ([onMapClick]) và kéo bản đồ ([onMapMoveStateChanged]).
 * - Khi cả 2 điểm start & end được chọn: vẽ đường nét đứt (dashed polyline) nối từ start đến end và cắm 2 marker cố định.
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
    fitRouteBounds: Boolean = false,
    mapViewportState: MapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(106.7009, 10.7769))
            zoom(14.0)
            pitch(0.0)
        }
    },
    onLocationUpdate: ((LocationData) -> Unit)? = null,
    onMapClick: ((Point) -> Unit)? = null,
    onMapMoveStateChanged: ((isMoving: Boolean) -> Unit)? = null,
    onCameraIdle: ((latitude: Double, longitude: Double) -> Unit)? = null,
    centerOverlay: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val primaryColor = AppTheme.colors.navyBlue
    val currentOnCameraCenterChanged by rememberUpdatedState(onCameraCenterChanged)
    val currentOnLocationUpdate by rememberUpdatedState(onLocationUpdate)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnMapMoveStateChanged by rememberUpdatedState(onMapMoveStateChanged)
    val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)
    val currentCameraBottomPadding by rememberUpdatedState(cameraBottomPadding)

    val isPermissionGrantedInitially = remember(context) {
        (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED)
    }
    var hasLocationPermission by remember { mutableStateOf(isPermissionGrantedInitially) }

    if (showUserLocation) {
        MapPermissionHandler(
            onPermissionGranted = { hasLocationPermission = true },
            onPermissionDenied = { hasLocationPermission = false },
        )
    }

    val mapInitOptions = rememberTextureMapInitOptions("route_picker_map")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MapPlaceholderBackgroundColor)
    ) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            composeMapInitOptions = mapInitOptions,
            mapViewportState = mapViewportState,
        ) {
            // 1. Đồng bộ Camera Bottom Padding theo BottomSheet (không gián đoạn flyTo animation đang chạy)
            MapEffect(cameraBottomPadding) { mapView ->
                val mapboxMap = mapView.mapboxMap
                val currentCamera = mapboxMap.cameraState
                val maxSafeBottom = if (mapView.height > 0) {
                    (mapView.height * 0.88).coerceAtLeast(0.0)
                } else {
                    cameraBottomPadding.toDouble().coerceAtLeast(0.0)
                }
                val targetBottom = cameraBottomPadding.toDouble().coerceIn(0.0, maxSafeBottom)
                if (abs(currentCamera.padding.bottom - targetBottom) >= 2.0) {
                    mapView.camera.easeTo(
                        cameraOptions {
                            padding(EdgeInsets(0.0, 0.0, targetBottom, 0.0))
                        },
                        MapAnimationOptions.mapAnimationOptions {
                            duration(0L)
                            owner("BOTTOM_SHEET_PADDING")
                        },
                    )
                }
            }

            // 2. Setup User Location Puck theo quyền vị trí (không ẩn bản đồ khi từ chối GPS)
            MapEffect(showUserLocation, hasLocationPermission) { mapView ->
                val locationPlugin = mapView.location
                val shouldEnableLocation = showUserLocation && hasLocationPermission

                if (!shouldEnableLocation) {
                    locationPlugin.updateSettings {
                        enabled = false
                    }
                    return@MapEffect
                }

                locationPlugin.updateSettings {
                    locationPuck = createZoomScaled2DPuck(withBearing = true)
                    enabled = true
                    puckBearing = PuckBearing.HEADING
                    puckBearingEnabled = true
                    showAccuracyRing = true
                }

                val indicatorListener = OnIndicatorPositionChangedListener { point ->
                    currentOnLocationUpdate?.invoke(
                        LocationData(
                            latitude = point.latitude(),
                            longitude = point.longitude(),
                        ),
                    )
                }
                locationPlugin.addOnIndicatorPositionChangedListener(indicatorListener)

                try {
                    awaitCancellation()
                } finally {
                    locationPlugin.removeOnIndicatorPositionChangedListener(indicatorListener)
                }
            }

            // 3. Lắng nghe Camera, Map Click và Gesture kéo bản đồ (khởi tạo 1 lần và dọn dẹp đúng lifecycle)
            MapEffect(Unit) { mapView ->
                val mapboxMap = mapView.mapboxMap

                val cameraCallback = CameraChangedCallback {
                    val center = mapboxMap.cameraState.center
                    currentOnCameraCenterChanged(center.latitude(), center.longitude())
                }
                val cancellableCamera = mapboxMap.subscribeCameraChanged(cameraCallback)

                val mapIdleCallback = MapIdleCallback {
                    val center = mapboxMap.cameraState.center
                    currentOnCameraIdle?.invoke(center.latitude(), center.longitude())
                }
                val cancellableIdle = mapboxMap.subscribeMapIdle(mapIdleCallback)

                val clickListener = OnMapClickListener { point ->
                    currentOnMapClick?.invoke(point)
                    true
                }
                mapView.gestures.addOnMapClickListener(clickListener)

                val moveListener = object : OnMoveListener {
                    override fun onMoveBegin(detector: MoveGestureDetector) {
                        currentOnMapMoveStateChanged?.invoke(true)
                    }

                    override fun onMove(detector: MoveGestureDetector): Boolean = false

                    override fun onMoveEnd(detector: MoveGestureDetector) {
                        currentOnMapMoveStateChanged?.invoke(false)
                        val center = mapboxMap.cameraState.center
                        currentOnCameraCenterChanged(center.latitude(), center.longitude())
                    }
                }
                mapView.gestures.addOnMoveListener(moveListener)

                try {
                    awaitCancellation()
                } finally {
                    cancellableCamera.cancel()
                    cancellableIdle.cancel()
                    mapView.gestures.removeOnMapClickListener(clickListener)
                    mapView.gestures.removeOnMoveListener(moveListener)
                }
            }

            // 4. Vẽ đường nét đứt (Dashed Polyline) và 2 Marker cố định tại Start và End
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
                            lineDasharray(listOf(2.0, 2.0))
                        }
                        style.addLayer(layer)
                    }

                    val pointFeatures = mutableListOf<Feature>()
                    if (startPoint != null) {
                        val feat = Feature.fromGeometry(
                            Point.fromLngLat(startPoint.longitude, startPoint.latitude),
                        )
                        feat.addStringProperty("point_type", "start")
                        pointFeatures.add(feat)
                    }
                    if (endPoint != null) {
                        val feat = Feature.fromGeometry(
                            Point.fromLngLat(endPoint.longitude, endPoint.latitude),
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

                    if (!style.styleLayerExists(PICKER_POINTS_OUTER_LAYER_ID)) {
                        val outerLayer = circleLayer(PICKER_POINTS_OUTER_LAYER_ID, PICKER_POINTS_SOURCE_ID) {
                            circleRadius(11.0)
                            circleColor(
                                match {
                                    get { literal("point_type") }
                                    stop {
                                        literal("start")
                                        literal("#10B981")
                                    }
                                    stop {
                                        literal("end")
                                        literal("#EF4444")
                                    }
                                    literal("#3B82F6")
                                },
                            )
                            circleStrokeWidth(3.0)
                            circleStrokeColor("#FFFFFF")
                        }
                        style.addLayer(outerLayer)
                    }

                    if (!style.styleLayerExists(PICKER_POINTS_INNER_LAYER_ID)) {
                        val innerLayer = circleLayer(PICKER_POINTS_INNER_LAYER_ID, PICKER_POINTS_SOURCE_ID) {
                            circleRadius(4.5)
                            circleColor("#FFFFFF")
                        }
                        style.addLayer(innerLayer)
                    }
                }
            }

            // 5. Khi ở chế độ xem phương án di chuyển (fitRouteBounds), tự động zoom bao trọn Start & End an toàn
            MapEffect(startPoint, endPoint, fitRouteBounds) { mapView ->
                if (!fitRouteBounds || startPoint == null || endPoint == null) return@MapEffect
                val mapboxMap = mapView.mapboxMap
                val points = listOf(
                    Point.fromLngLat(startPoint.longitude, startPoint.latitude),
                    Point.fromLngLat(endPoint.longitude, endPoint.latitude),
                )
                val mapHeight = mapView.height.toDouble()
                val safeTop = 140.0
                val maxSafeBottom = if (mapHeight > 320.0) {
                    (mapHeight - safeTop - 160.0).coerceAtLeast(140.0)
                } else {
                    140.0
                }
                val safeBottom = (currentCameraBottomPadding.toDouble() + 80.0)
                    .coerceIn(140.0, maxSafeBottom)

                val camera = mapboxMap.cameraForCoordinates(
                    points,
                    EdgeInsets(safeTop, 100.0, safeBottom, 100.0),
                    null,
                    null,
                )
                mapView.camera.easeTo(
                    camera,
                    MapAnimationOptions.mapAnimationOptions {
                        duration(500L)
                    },
                )
            }
        }

        // 6. Center overlay (Start/End Pin) luôn căn khớp tâm camera thực theo cameraBottomPadding
        if (centerOverlay != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(0, -((cameraBottomPadding / 2f).roundToInt()))
                    },
                contentAlignment = Alignment.Center,
            ) {
                centerOverlay()
            }
        }
    }
}
