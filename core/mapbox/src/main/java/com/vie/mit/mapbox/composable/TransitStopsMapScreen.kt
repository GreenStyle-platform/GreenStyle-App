package com.vie.mit.mapbox.composable

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraChangedCallback
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.StopFeatureClick
import com.vie.mit.mapbox.model.TransitClusterConfig
import com.vie.mit.mapbox.model.TransitMapViewportInfo
import com.vie.mit.mapbox.model.TransitSourceData
import com.vie.mit.mapbox.model.TransitTypeConfig
import com.vie.mit.mapbox.model.toLocationData
import com.vie.mit.mapbox.permission.MapPermissionHandler
import com.vie.mit.mapbox.util.bitmapFromDrawableRes
import kotlinx.coroutines.awaitCancellation
import timber.log.Timber
import com.mapbox.maps.MapboxMap as MapboxMapController

/**
 * Pure UI Mapbox Composable for displaying GTFS public transit stops with clustering.
 *
 * Adheres strictly to Clean Architecture:
 * - Does NOT fetch or manage network data or ViewModels directly.
 * - Accepts [transitSources], visual configs [transitConfigs], and callbacks.
 * - Manages Mapbox sources, layers, clustering, icon rendering, user location puck, and interactions.
 */
@Composable
fun TransitStopsMapScreen(
    modifier: Modifier = Modifier,
    transitSources: List<TransitSourceData>,
    transitConfigs: List<TransitTypeConfig>,
    mapViewportState: MapViewportState = rememberMapViewportState(),
    clusterConfig: TransitClusterConfig = remember { TransitClusterConfig() },
    showUserLocation: Boolean = true,
    followPuckOnStart: Boolean = true,
    onLocationUpdate: ((LocationData) -> Unit)? = null,
    onStopClick: ((StopFeatureClick) -> Unit)? = null,
    onClusterClick: ((Point, expansionZoom: Double) -> Unit)? = null,
    onViewportChanged: ((TransitMapViewportInfo) -> Unit)? = null,
) {
    val context = LocalContext.current
    val currentOnLocationUpdate by rememberUpdatedState(onLocationUpdate)
    val currentOnStopClick by rememberUpdatedState(onStopClick)
    val currentOnClusterClick by rememberUpdatedState(onClusterClick)
    val currentOnViewportChanged by rememberUpdatedState(onViewportChanged)

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
                MapEffect(transitSources, transitConfigs, clusterConfig, showUserLocation) { mapView ->
                    val mapboxMap = mapView.mapboxMap

                    mapboxMap.getStyle { style ->
                        setupTransitIcons(context, style, transitConfigs)
                        setupTransitSourcesAndLayers(style, transitSources, transitConfigs, clusterConfig)
                    }

                    // 1. Setup User Location Puck if enabled
                    var indicatorListener: OnIndicatorPositionChangedListener? = null
                    if (showUserLocation) {
                        val locationPlugin = mapView.location
                        locationPlugin.updateSettings {
                            locationPuck = createDefault2DPuck(true)
                            enabled = true
                            puckBearing = PuckBearing.HEADING
                            puckBearingEnabled = true
                            showAccuracyRing = true
                        }
                        indicatorListener = OnIndicatorPositionChangedListener { point ->
                            val loc = point.toLocationData()
                            currentOnLocationUpdate?.invoke(loc)
                        }
                        locationPlugin.addOnIndicatorPositionChangedListener(indicatorListener)

                        if (followPuckOnStart) {
                            mapViewportState.transitionToFollowPuckState()
                        }
                    }

                    // 2. Register Map Click Listener for clusters & unclustered stops
                    val clickListener = OnMapClickListener { point ->
                        val screenCoordinate = mapboxMap.pixelForCoordinate(point)
                        handleMapClick(
                            mapboxMap = mapboxMap,
                            screenCoordinate = screenCoordinate,
                            clickPoint = point,
                            transitConfigs = transitConfigs,
                            onStopClick = { currentOnStopClick?.invoke(it) },
                            onClusterClick = { clusterPoint, expansionZoom ->
                                mapViewportState.flyTo(
                                    cameraOptions {
                                        center(clusterPoint)
                                        zoom(expansionZoom + 0.5)
                                    },
                                    MapAnimationOptions.mapAnimationOptions {
                                        duration(500)
                                    }
                                )
                                currentOnClusterClick?.invoke(clusterPoint, expansionZoom)
                            }
                        )
                        true
                    }
                    mapView.gestures.addOnMapClickListener(clickListener)

                    // 3. Register Camera change listener to notify camera viewport
                    val cameraChangedCallback = CameraChangedCallback {
                        val cameraState = mapboxMap.cameraState
                        val cameraOpts = cameraOptions {
                            center(cameraState.center)
                            zoom(cameraState.zoom)
                            pitch(cameraState.pitch)
                            bearing(cameraState.bearing)
                            padding(cameraState.padding)
                        }
                        val bounds = mapboxMap.coordinateBoundsForCamera(cameraOpts)
                        val viewportInfo = TransitMapViewportInfo(
                            minLatitude = bounds.south(),
                            minLongitude = bounds.west(),
                            maxLatitude = bounds.north(),
                            maxLongitude = bounds.east(),
                            zoom = cameraState.zoom,
                            centerLatitude = cameraState.center.latitude(),
                            centerLongitude = cameraState.center.longitude(),
                        )
                        currentOnViewportChanged?.invoke(viewportInfo)
                    }
                    val cancellableCamera = mapboxMap.subscribeCameraChanged(cameraChangedCallback)

                    try {
                        awaitCancellation()
                    } finally {
                        mapView.gestures.removeOnMapClickListener(clickListener)
                        cancellableCamera.cancel()
                        indicatorListener?.let {
                            mapView.location.removeOnIndicatorPositionChangedListener(it)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Registers transit icons from Vector XML resources into Mapbox Style image cache.
 */
private fun setupTransitIcons(
    context: Context,
    style: Style,
    transitConfigs: List<TransitTypeConfig>,
) {
    transitConfigs.forEach { config ->
        if (!style.styleSourceExists(config.iconName)) {
            val bitmap = bitmapFromDrawableRes(context, config.iconResId)
            if (bitmap != null) {
                style.addImage(config.iconName, bitmap)
                Timber.d("TransitStopsMapScreen: registered icon image '${config.iconName}'")
            } else {
                Timber.w("TransitStopsMapScreen: failed to decode drawable for icon '${config.iconName}'")
            }
        }
    }
}

/**
 * Sets up or updates GeoJSON sources and corresponding cluster & symbol layers for each transit type.
 */
private fun setupTransitSourcesAndLayers(
    style: Style,
    transitSources: List<TransitSourceData>,
    transitConfigs: List<TransitTypeConfig>,
    clusterConfig: TransitClusterConfig,
) {
    val configMap = transitConfigs.associateBy { it.typeId }

    transitSources.forEach { sourceData ->
        val config = configMap[sourceData.typeId] ?: return@forEach
        val sourceId = getSourceIdForType(sourceData.typeId)
        val clusterCircleLayerId = getClusterCircleLayerIdForType(sourceData.typeId)
        val clusterCountLayerId = getClusterCountLayerIdForType(sourceData.typeId)
        val stopSymbolLayerId = getStopSymbolLayerIdForType(sourceData.typeId)

        // 1. Add or Update GeoJsonSource
        val existingSource = style.getSourceAs<GeoJsonSource>(sourceId)
        if (existingSource != null) {
            existingSource.data(sourceData.geoJson)
            Timber.d("TransitStopsMapScreen: updated data for GeoJsonSource '$sourceId'")
        } else {
            val newSource = geoJsonSource(sourceId) {
                data(sourceData.geoJson)
                cluster(true)
                clusterRadius(clusterConfig.clusterRadius)
                clusterMaxZoom(clusterConfig.clusterMaxZoom)
            }
            style.addSource(newSource)
            Timber.d("TransitStopsMapScreen: added GeoJsonSource '$sourceId' with clustering")
        }

        val targetVisibility = if (sourceData.isVisible) Visibility.VISIBLE else Visibility.NONE

        // 2. Add Cluster Circle Layer (if not exists)
        if (!style.styleLayerExists(clusterCircleLayerId)) {
            val circleLayer = circleLayer(clusterCircleLayerId, sourceId) {
                filter(Expression.fromRaw("""["has", "point_count"]"""))
                circleColor(config.clusterColor)
                circleRadius(
                    Expression.fromRaw(
                        """["step", ["get", "point_count"], 18.0, 10, 24.0, 50, 30.0, 100, 36.0]"""
                    )
                )
                circleStrokeWidth(2.5)
                circleStrokeColor("#FFFFFF")
                circleOpacity(0.9)
                visibility(targetVisibility)
            }
            style.addLayer(circleLayer)
        } else {
            style.getLayer(clusterCircleLayerId)?.visibility(targetVisibility)
        }

        // 3. Add Cluster Text Count Layer (if not exists)
        if (!style.styleLayerExists(clusterCountLayerId)) {
            val countLayer = symbolLayer(clusterCountLayerId, sourceId) {
                filter(Expression.fromRaw("""["has", "point_count"]"""))
                textField(Expression.fromRaw("""["get", "point_count_abbreviated"]"""))
                textSize(12.0)
                textColor(config.clusterTextColor)
                textIgnorePlacement(true)
                textAllowOverlap(true)
                visibility(targetVisibility)
            }
            style.addLayer(countLayer)
        } else {
            style.getLayer(clusterCountLayerId)?.visibility(targetVisibility)
        }

        // 4. Add Unclustered Stop Symbol Layer (if not exists)
        if (!style.styleLayerExists(stopSymbolLayerId)) {
            val stopSymbolLayer = symbolLayer(stopSymbolLayerId, sourceId) {
                filter(Expression.fromRaw("""["!", ["has", "point_count"]]"""))
                iconImage(config.iconName)
                iconSize(config.iconSize)
                iconAllowOverlap(true)
                iconIgnorePlacement(false)
                visibility(targetVisibility)
            }
            style.addLayer(stopSymbolLayer)
        } else {
            style.getLayer(stopSymbolLayerId)?.visibility(targetVisibility)
        }
    }
}

/**
 * Handles clicks on either a cluster circle or an individual stop symbol.
 */
private fun handleMapClick(
    mapboxMap: MapboxMapController,
    screenCoordinate: ScreenCoordinate,
    clickPoint: Point,
    transitConfigs: List<TransitTypeConfig>,
    onStopClick: (StopFeatureClick) -> Unit,
    onClusterClick: (Point, Double) -> Unit,
) {
    val clusterLayerIds = transitConfigs.map { getClusterCircleLayerIdForType(it.typeId) }
    val stopLayerIds = transitConfigs.map { getStopSymbolLayerIdForType(it.typeId) }
    val allLayerIds = clusterLayerIds + stopLayerIds

    val queryOptions = RenderedQueryOptions(allLayerIds, null)
    val queryGeometry = RenderedQueryGeometry(screenCoordinate)

    mapboxMap.queryRenderedFeatures(queryGeometry, queryOptions) { expected ->
        val features = expected.value ?: return@queryRenderedFeatures
        if (features.isEmpty()) return@queryRenderedFeatures

        val topFeature = features.first().queriedFeature.feature
        val topLayerId = features.first().layers.firstOrNull() ?: ""

        // Check if a cluster was clicked
        if (topFeature.hasNonNullValueForProperty("point_count")) {
            val matchingConfig = transitConfigs.find { getClusterCircleLayerIdForType(it.typeId) == topLayerId }
            val sourceId = getSourceIdForType(matchingConfig?.typeId ?: "")

            mapboxMap.getGeoJsonClusterExpansionZoom(sourceId, topFeature) { zoomResult ->
                val expansionZoom = zoomResult.value?.toString()?.toDoubleOrNull()
                val centerPoint = (topFeature.geometry() as? Point) ?: clickPoint
                if (expansionZoom != null) {
                    onClusterClick(centerPoint, expansionZoom)
                }
            }
            return@queryRenderedFeatures
        }

        // Otherwise, an individual stop was clicked
        val matchingConfig = transitConfigs.find { getStopSymbolLayerIdForType(it.typeId) == topLayerId }
        val typeId = matchingConfig?.typeId ?: topFeature.getStringProperty("type") ?: ""
        val stopId = topFeature.id()
            ?: topFeature.getStringProperty("stop_id")
            ?: topFeature.getStringProperty("id")
            ?: ""
        val name = topFeature.getStringProperty("name")
            ?: topFeature.getStringProperty("stop_name")

        val propertyMap = mutableMapOf<String, String>()
        topFeature.properties()?.entrySet()?.forEach { entry ->
            propertyMap[entry.key] = entry.value.asString
        }

        val point = (topFeature.geometry() as? Point) ?: clickPoint

        onStopClick(
            StopFeatureClick(
                stopId = stopId,
                typeId = typeId,
                name = name,
                point = point,
                properties = propertyMap,
            )
        )
    }
}

fun getSourceIdForType(typeId: String) = "source_transit_$typeId"
fun getClusterCircleLayerIdForType(typeId: String) = "layer_cluster_circle_$typeId"
fun getClusterCountLayerIdForType(typeId: String) = "layer_cluster_count_$typeId"
fun getStopSymbolLayerIdForType(typeId: String) = "layer_stop_symbol_$typeId"
