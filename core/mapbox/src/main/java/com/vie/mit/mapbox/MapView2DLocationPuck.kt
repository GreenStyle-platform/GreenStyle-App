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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.Marker
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import timber.log.Timber

@Composable
fun MapView2DLocationPuck(
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
    var showDeniedUI by remember { mutableStateOf(false) }
    val mapViewportState = rememberMapViewportState()

    // Permission handler for initial request or changes
    MapPermissionHandler(
        onPermissionGranted = {
            showMap = true
            showDeniedUI = false
        },
        onPermissionDenied = {
            showMap = false
            showDeniedUI = true
        }
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (showMap) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
            ) {
                MapContent(mapViewportState = mapViewportState)
                
                Marker(
                    point = Point.fromLngLat(105.0, 21.0)
                )
                
                val marker = rememberIconImage(
                    key = "red-marker",
                    painter = painterResource(id = com.vie.mit.common.R.drawable.ic_driver)
                )

                PointAnnotation(point = Point.fromLngLat(18.06, 59.31)) {
                    iconImage = marker
                    // Có ther khai báo 1 biến pointAnnotationInteractionsState để có thể update config cho PointAnnocation
                    // Và thực hiện truyền vào cho PointAnnotation.
                    interactionsState.onClicked {
    // do something when clicked
    true
  }.onLongClicked {
    // do something when long clicked
    true
  }.onDragged {
    // do something when dragged
  }.also{
                        it.isDraggable = true
                    }
                }

                PolylineAnnotation(
                    points = listOf(
                        Point.fromLngLat(18.06, 59.31),
                        Point.fromLngLat(105.0, 21.0)
                    )
                ) {
                    lineColor = Color(0xffee4e8b)
                    lineWidth = 5.0
                }
            }
        }
    }
}

@Composable
@MapboxMapComposable
fun MapContent(
    mapViewportState: MapViewportState
) {
    MapEffect(Unit) { mapView ->
        mapView.location.updateSettings {
            locationPuck = LocationPuck2D(
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )

            enabled = true
            puckBearing = PuckBearing.HEADING
            puckBearingEnabled = true
            showAccuracyRing = true
        }

        val locationPlugin = mapView.location

        locationPlugin.addOnIndicatorPositionChangedListener { point ->
            Timber.d("PhucTH location: ${point.latitude()} ${point.longitude()}")
        }

        mapViewportState.transitionToFollowPuckState(
            followPuckViewportStateOptions = FollowPuckViewportStateOptions.Builder()
                .bearing(FollowPuckViewportStateBearing.Constant(0.0))
                .padding(
                    EdgeInsets(
                        200.0 * mapView.context.resources.displayMetrics.density,
                        0.0,
                        0.0,
                        0.0
                    )
                )
                .build(),
        ) {
            // the transition has been completed
        }
    }
}
