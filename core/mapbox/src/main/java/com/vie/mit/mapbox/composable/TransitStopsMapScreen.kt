@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.vie.mit.mapbox.composable

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraChangedCallback
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.StopFeatureClick
import com.vie.mit.mapbox.model.TransitClusterConfig
import com.vie.mit.mapbox.model.TransitMapViewportInfo
import com.vie.mit.mapbox.model.TransitSourceData
import com.vie.mit.mapbox.model.TransitTypeConfig
import com.vie.mit.mapbox.model.distanceTo
import com.vie.mit.mapbox.model.toLocationData
import com.vie.mit.mapbox.permission.MapPermissionHandler
import com.vie.mit.mapbox.util.createZoomScaled2DPuck
import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt

const val distanceThreshold:Float = 30f

/**
 * Pure UI Mapbox Composable for displaying GTFS public transit stops with clustering.
 *
 * Adheres strictly to Clean Architecture:
 * - Does NOT fetch or manage network data or ViewModels directly.
 * - Accepts [transitSources], visual configs [transitConfigs], and callbacks.
 * - Delegates layer rendering, location puck setup, and camera callbacks to modular helpers.
 * - Free camera movement: Does NOT lock/snap camera to user location on drag.
 */
@Composable
fun TransitStopsMapScreen(
    modifier: Modifier = Modifier,
    transitSources: List<TransitSourceData>,
    transitConfigs: List<TransitTypeConfig>,
    mapViewportState: MapViewportState = rememberMapViewportState(),
    clusterConfig: TransitClusterConfig = remember { TransitClusterConfig() },
    showUserLocation: Boolean = true,
    showCenterPin: Boolean = true,
    cameraBottomPadding: Float = 0f,
    minZoom: Double? = null,
    maxZoom: Double? = null,
    centerPinContent: (@Composable () -> Unit)? = null,
    onLocationUpdate: ((LocationData) -> Unit)? = null,
    onStopClick: ((StopFeatureClick) -> Unit)? = null,
    onClusterClick: ((Point, expansionZoom: Double) -> Unit)? = null,
    onViewportChanged: ((TransitMapViewportInfo) -> Unit)? = null,
    onCameraGestureRelease: ((TransitMapViewportInfo) -> Unit)? = null,
    onMapLongClick: ((Point) -> Unit)? = null,
    onMapClick: ((Point) -> Unit)? = null,
) {
    val context = LocalContext.current
    val currentOnLocationUpdate by rememberUpdatedState(onLocationUpdate)
    val currentOnStopClick by rememberUpdatedState(onStopClick)
    val currentOnClusterClick by rememberUpdatedState(onClusterClick)
    val currentOnViewportChanged by rememberUpdatedState(onViewportChanged)
    val currentOnCameraGestureRelease by rememberUpdatedState(onCameraGestureRelease)
    val currentOnMapLongClick by rememberUpdatedState(onMapLongClick)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentTransitConfigs by rememberUpdatedState(transitConfigs)
    val userLocationRef = remember { AtomicReference<LocationData?>(null) }

    val isPermissionGrantedInitially = remember(context) {
        (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED)
    }
    var showMap by remember { mutableStateOf(if (showUserLocation) isPermissionGrantedInitially else true) }
    var isCameraFocusedOnUserLocation by remember { mutableStateOf(false) }

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
                // Thiết lập giới hạn Zoom (minZoom, maxZoom)
                MapEffect(minZoom, maxZoom) { mapView ->
                    if (minZoom != null || maxZoom != null) {
                        val boundsBuilder = CameraBoundsOptions.Builder()
                        minZoom?.let { boundsBuilder.minZoom(it) }
                        maxZoom?.let { boundsBuilder.maxZoom(it) }
                        mapView.mapboxMap.setBounds(boundsBuilder.build())
                    }
                }

                // Đồng bộ Camera Bottom Padding (Phương pháp 2: Mapbox EdgeInsets)
                MapEffect(cameraBottomPadding) { mapView ->
                    val mapboxMap = mapView.mapboxMap
                    val currentCamera = mapboxMap.cameraState
                    val targetBottom = cameraBottomPadding.toDouble().coerceAtLeast(0.0)
                    if (abs(currentCamera.padding.bottom - targetBottom) >= 2.0) {
                        val currentCenter = currentCamera.center
                        mapboxMap.setCamera(
                            cameraOptions {
                                center(currentCenter)
                                zoom(currentCamera.zoom)
                                pitch(currentCamera.pitch)
                                bearing(currentCamera.bearing)
                                padding(EdgeInsets(0.0, 0.0, targetBottom, 0.0))
                            }
                        )
                    }
                }

                // 2. Configure GTFS Icons, Sources & Layers (chỉ cập nhật data layer, không đụng tới listeners)
                MapEffect(transitSources, transitConfigs, clusterConfig) { mapView ->
                    val mapboxMap = mapView.mapboxMap
                    mapboxMap.getStyle { style ->
                        setupTransitIcons(context, style, transitConfigs)
                        setupTransitSourcesAndLayers(style, transitSources, transitConfigs, clusterConfig)
                    }
                }

                // 3. Location Puck & Map Gestures/Camera Listeners (chỉ khởi tạo 1 lần theo showUserLocation)
                MapEffect(showUserLocation) { mapView ->
                    val mapboxMap = mapView.mapboxMap

                    // 3.1 Setup User Location Puck if enabled
                    var indicatorListener: OnIndicatorPositionChangedListener? = null
                    if (showUserLocation) {
                        val locationPlugin = mapView.location
                        locationPlugin.updateSettings {
                            locationPuck = createZoomScaled2DPuck(withBearing = true)
                            enabled = true
                            puckBearing = PuckBearing.HEADING
                            puckBearingEnabled = true
                            showAccuracyRing = true
                        }
                        indicatorListener = OnIndicatorPositionChangedListener { point ->
                            val loc = point.toLocationData()
                            userLocationRef.set(loc)
                            val centerLoc = mapboxMap.cameraState.center.toLocationData()
                            val isNear = centerLoc.distanceTo(loc) < distanceThreshold
                            if (isCameraFocusedOnUserLocation != isNear) {
                                isCameraFocusedOnUserLocation = isNear
                            }
                            currentOnLocationUpdate?.invoke(loc)
                        }
                        locationPlugin.addOnIndicatorPositionChangedListener(indicatorListener)
                    }

                    // 3.2 Register Map Click Listener for clusters & stops
                    val clickListener = OnMapClickListener { point ->
                        val screenCoordinate = mapboxMap.pixelForCoordinate(point)
                        handleMapClick(
                            mapboxMap = mapboxMap,
                            screenCoordinate = screenCoordinate,
                            clickPoint = point,
                            transitConfigs = currentTransitConfigs,
                            onStopClick = { currentOnStopClick?.invoke(it) },
                            onClusterClick = { clusterPoint, expansionZoom ->
                                mapViewportState.flyTo(
                                    cameraOptions {
                                        center(clusterPoint)
                                        zoom(expansionZoom + 2)
                                    },
                                    MapAnimationOptions.mapAnimationOptions {
                                        duration(500)
                                    }
                                )
                                currentOnClusterClick?.invoke(clusterPoint, expansionZoom)
                            }
                        )
                        currentOnMapClick?.invoke(point)
                        true
                    }
                    mapView.gestures.addOnMapClickListener(clickListener)

                    // 3.3 Register Long Click and Move Listeners for drag release
                    var isLongPressActive = false

                    val longClickListener = OnMapLongClickListener { point ->
                        isLongPressActive = true
                        currentOnMapLongClick?.invoke(point)
                        false
                    }
                    mapView.gestures.addOnMapLongClickListener(longClickListener)

                    val moveListener = object : OnMoveListener {
                        override fun onMoveBegin(detector: MoveGestureDetector) {
                            // Map drag/scroll has started
                        }

                        override fun onMove(detector: MoveGestureDetector): Boolean {
                            return false
                        }

                        override fun onMoveEnd(detector: MoveGestureDetector) {
                            // User released finger after panning/dragging
                            val viewportInfo = mapboxMap.getCurrentViewportInfo()
                            currentOnCameraGestureRelease?.invoke(viewportInfo)
                            isLongPressActive = false
                        }
                    }
                    mapView.gestures.addOnMoveListener(moveListener)

                    // 3.4 Register Camera change listener to notify viewport changes
                    val cameraChangedCallback = CameraChangedCallback {
                        val cameraState = mapboxMap.cameraState
                        val cameraCenterLoc = cameraState.center.toLocationData()
                        val uLoc = userLocationRef.get()
                        if (uLoc != null) {
                            val isNear = cameraCenterLoc.distanceTo(uLoc) < 15f
                            if (isCameraFocusedOnUserLocation != isNear) {
                                isCameraFocusedOnUserLocation = isNear
                            }
                        }
                        val viewportInfo = mapboxMap.getCurrentViewportInfo()
                        currentOnViewportChanged?.invoke(viewportInfo)
                    }
                    val cancellableCamera = mapboxMap.subscribeCameraChanged(cameraChangedCallback)

                    try {
                        awaitCancellation()
                    } finally {
                        mapView.gestures.removeOnMapClickListener(clickListener)
                        mapView.gestures.removeOnMapLongClickListener(longClickListener)
                        mapView.gestures.removeOnMoveListener(moveListener)
                        cancellableCamera.cancel()
                        indicatorListener?.let {
                            mapView.location.removeOnIndicatorPositionChangedListener(it)
                        }
                    }
                }
            }

            // 6. Center pin overlay fixed at map camera center (offset theo cameraBottomPadding để luôn ở tâm)
            MapCenterPin(
                showPin = showCenterPin && !isCameraFocusedOnUserLocation,
                customContent = centerPinContent,
                modifier = Modifier.offset {
                    IntOffset(0, -((cameraBottomPadding / 2f).roundToInt()))
                }
            )
        }
    }
}

/**
 * Computes [TransitMapViewportInfo] for the current camera state of this [MapboxMap].
 */
fun MapboxMap.getCurrentViewportInfo(): TransitMapViewportInfo {
    val currentCamera = this.cameraState
    val cameraOpts = cameraOptions {
        center(currentCamera.center)
        zoom(currentCamera.zoom)
        pitch(0.0)
        bearing(currentCamera.bearing)
        padding(currentCamera.padding)
    }
    val bounds = this.coordinateBoundsForCamera(cameraOpts)
    return TransitMapViewportInfo(
        minLatitude = bounds.south(),
        minLongitude = bounds.west(),
        maxLatitude = bounds.north(),
        maxLongitude = bounds.east(),
        zoom = currentCamera.zoom,
        centerLatitude = currentCamera.center.latitude(),
        centerLongitude = currentCamera.center.longitude(),
    )
}
