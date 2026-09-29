package com.vie.mit.mapbox

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
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.LocationPuck3D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.location

@Composable
fun MapView3D(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPermissionGrantedInitially = remember(context) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    var showMap by remember { mutableStateOf(isPermissionGrantedInitially) }
    
    val mapViewportState: MapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(105.8342, 21.0278))
            pitch(60.0)
            zoom(15.0)
        }
    }

    MapPermissionHandler(
        onPermissionGranted = { showMap = true },
        onPermissionDenied = { showMap = false }
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (showMap) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                // Cách thiết lập style đúng cho v11: dùng URI string trực tiếp
            ) {
                MapContent3D(mapViewportState = mapViewportState)
            }
        }
    }
}

@Composable
@MapboxMapComposable
fun MapContent3D(
    mapViewportState: MapViewportState
) {
    MapEffect(Unit) { mapView ->
        mapView.location.updateSettings {
            enabled = true
            locationPuck = LocationPuck3D(
                modelUri = "asset://sportcar.glb",
                modelScale = listOf(20.0f, 20.0f, 20.0f),
                modelRotation = listOf(0.0f, 0.0f, 180.0f)
            )
            puckBearing = PuckBearing.HEADING
            puckBearingEnabled = true
        }
        mapViewportState.transitionToFollowPuckState()
    }
}
