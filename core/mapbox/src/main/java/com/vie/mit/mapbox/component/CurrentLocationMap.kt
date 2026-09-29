package com.vie.mit.mapbox.component

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
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.vie.mit.mapbox.MapPermissionHandler
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.distanceTo
import com.vie.mit.mapbox.model.toLocationData
import kotlinx.coroutines.awaitCancellation
import timber.log.Timber

/**
 * Simplified overload for feature modules that do not depend directly on Mapbox SDK types.
 *
 * @param modifier Modifier for the map layout.
 * @param followPuck Whether the camera should automatically track the location puck.
 * @param showAccuracyRing Whether to show an accuracy ring around the location puck.
 * @param minDistanceThresholdMeters Minimum movement in meters before triggering [onLocationUpdate] (default 0f = notify all changes).
 * @param onLocationUpdate Callback invoked when the user's location updates as [LocationData].
 * @param onPermissionDenied Callback invoked when location permissions are denied.
 */
@Composable
fun CurrentLocationMap(
    modifier: Modifier = Modifier,
    followPuck: Boolean = true,
    showAccuracyRing: Boolean = true,
    minDistanceThresholdMeters: Float = 0f,
    onLocationUpdate: ((LocationData) -> Unit)? = null,
    onPermissionDenied: (() -> Unit)? = null,
) {
    CurrentLocationMap(
        modifier = modifier,
        mapViewportState = rememberMapViewportState(),
        followPuck = followPuck,
        showAccuracyRing = showAccuracyRing,
        puckBearing = PuckBearing.HEADING,
        minDistanceThresholdMeters = minDistanceThresholdMeters,
        onLocationUpdate = onLocationUpdate,
        onPermissionDenied = onPermissionDenied,
        content = {},
    )
}

/**
 * A Composable MapView component displaying the user's current location with a Mapbox 2D location puck.
 *
 * @param modifier Modifier for the map layout.
 * @param mapViewportState Controls camera positioning and viewport state.
 * @param followPuck Whether the camera should automatically track the location puck.
 * @param showAccuracyRing Whether to show an accuracy ring around the location puck.
 * @param puckBearing Specifies the bearing source for the puck (e.g., HEADING or COURSE).
 * @param minDistanceThresholdMeters Minimum movement in meters before triggering [onLocationUpdate] (default 0f = notify all changes).
 * @param onLocationUpdate Callback invoked when the user's location updates as [LocationData].
 * @param onPermissionDenied Callback invoked when location permissions are denied.
 * @param content Additional composable map content (markers, polylines, annotations).
 */
@Composable
fun CurrentLocationMap(
    modifier: Modifier = Modifier,
    mapViewportState: MapViewportState = rememberMapViewportState(),
    followPuck: Boolean = true,
    showAccuracyRing: Boolean = true,
    puckBearing: PuckBearing = PuckBearing.HEADING,
    minDistanceThresholdMeters: Float = 0f,
    onLocationUpdate: ((LocationData) -> Unit)? = null,
    onPermissionDenied: (() -> Unit)? = null,
    content: @Composable @MapboxMapComposable () -> Unit = {},
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
    var showMap by remember { mutableStateOf(isPermissionGrantedInitially) }

    MapPermissionHandler(
        onPermissionGranted = {
            showMap = true
        },
        onPermissionDenied = {
            showMap = false
            onPermissionDenied?.invoke()
        },
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (showMap) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
            ) {
                CurrentLocationMapContent(
                    mapViewportState = mapViewportState,
                    followPuck = followPuck,
                    showAccuracyRing = showAccuracyRing,
                    puckBearing = puckBearing,
                    minDistanceThresholdMeters = minDistanceThresholdMeters,
                    onLocationUpdate = onLocationUpdate,
                )
                content()
            }
        }
    }
}

@Composable
@MapboxMapComposable
private fun CurrentLocationMapContent(
    mapViewportState: MapViewportState,
    followPuck: Boolean,
    showAccuracyRing: Boolean,
    puckBearing: PuckBearing,
    minDistanceThresholdMeters: Float,
    onLocationUpdate: ((LocationData) -> Unit)?,
) {
    MapEffect(followPuck, showAccuracyRing, puckBearing) { mapView ->
        val locationPlugin = mapView.location

        locationPlugin.updateSettings {
            locationPuck = createDefault2DPuck(true)
            enabled = true
            this.puckBearing = puckBearing
            puckBearingEnabled = true
            this.showAccuracyRing = showAccuracyRing
        }

        var lastNotifiedLocation: LocationData? = null

        val listener = OnIndicatorPositionChangedListener { point ->
            val locationData = point.toLocationData()
            val lastLocation = lastNotifiedLocation
            if (minDistanceThresholdMeters <= 0f || lastLocation == null || lastLocation.distanceTo(locationData) >= minDistanceThresholdMeters) {
                lastNotifiedLocation = locationData
                Timber.d("CurrentLocationMap location: ${locationData.latitude}, ${locationData.longitude}")
                onLocationUpdate?.invoke(locationData)
            }
        }

        locationPlugin.addOnIndicatorPositionChangedListener(listener)

        if (followPuck) {
            mapViewportState.transitionToFollowPuckState()
        }

        try {
            awaitCancellation()
        } finally {
            locationPlugin.removeOnIndicatorPositionChangedListener(listener)
        }
    }
}
