package com.vie.mit.mapbox.composable

import android.content.Context
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
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
import com.vie.mit.mapbox.model.StopFeatureClick
import com.vie.mit.mapbox.model.TransitClusterConfig
import com.vie.mit.mapbox.model.TransitSourceData
import com.vie.mit.mapbox.model.TransitTypeConfig
import com.vie.mit.mapbox.util.bitmapFromDrawableRes
import timber.log.Timber

/**
 * Registers transit icons from Vector XML resources into Mapbox Style image cache.
 */
fun setupTransitIcons(
    context: Context,
    style: Style,
    transitConfigs: List<TransitTypeConfig>,
) {
    transitConfigs.forEach { config ->
        if (style.getStyleImage(config.iconName) == null) {
            val bitmap = bitmapFromDrawableRes(context, config.iconResId)
            if (bitmap != null) {
                style.addImage(config.iconName, bitmap)
                Timber.d("TransitMapStyleManager: registered icon image '${config.iconName}'")
            } else {
                Timber.w("TransitMapStyleManager: failed to decode drawable for icon '${config.iconName}'")
            }
        }
    }
}

/**
 * Sets up or updates GeoJSON sources and corresponding cluster & symbol layers for each transit type.
 */
fun setupTransitSourcesAndLayers(
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
        if (sourceData.geoJson.isNotBlank()) {
            val existingSource = style.getSourceAs<GeoJsonSource>(sourceId)
            if (existingSource != null) {
                existingSource.data(sourceData.geoJson)
                Timber.d("TransitMapStyleManager: updated data for GeoJsonSource '$sourceId'")
            } else {
                val newSource = geoJsonSource(sourceId) {
                    data(sourceData.geoJson)
                    cluster(true)
                    clusterRadius(clusterConfig.clusterRadius)
                    clusterMaxZoom(clusterConfig.clusterMaxZoom)
                }
                style.addSource(newSource)
                Timber.d("TransitMapStyleManager: added GeoJsonSource '$sourceId' with clustering")
            }
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
fun handleMapClick(
    mapboxMap: MapboxMap,
    screenCoordinate: ScreenCoordinate,
    clickPoint: Point,
    transitConfigs: List<TransitTypeConfig>,
    onStopClick: (StopFeatureClick) -> Unit,
    onClusterClick: (Point, Double) -> Unit,
) {
    val clusterCircleLayerIds = transitConfigs.map { getClusterCircleLayerIdForType(it.typeId) }
    val clusterCountLayerIds = transitConfigs.map { getClusterCountLayerIdForType(it.typeId) }
    val stopLayerIds = transitConfigs.map { getStopSymbolLayerIdForType(it.typeId) }
    val allLayerIds = clusterCircleLayerIds + clusterCountLayerIds + stopLayerIds

    val queryOptions = RenderedQueryOptions(allLayerIds, null)
    val queryGeometry = RenderedQueryGeometry(screenCoordinate)

    mapboxMap.queryRenderedFeatures(queryGeometry, queryOptions) { expected ->
        val features = expected.value ?: return@queryRenderedFeatures
        if (features.isEmpty()) return@queryRenderedFeatures

        val topFeature = features.first().queriedFeature.feature
        val topLayerId = features.first().layers.firstOrNull() ?: ""

        // Check if a cluster was clicked
        if (topFeature.hasNonNullValueForProperty("point_count")) {
            val matchingConfig = transitConfigs.find {
                getClusterCircleLayerIdForType(it.typeId) == topLayerId ||
                getClusterCountLayerIdForType(it.typeId) == topLayerId
            }
            val sourceId = getSourceIdForType(matchingConfig?.typeId ?: "")

            mapboxMap.getGeoJsonClusterExpansionZoom(sourceId, topFeature) { zoomResult ->
                val featureExtensionValue = zoomResult.value
                val bindgenValue = featureExtensionValue?.value
                val rawContent = bindgenValue?.contents
                val expansionZoom = (rawContent as? Number)?.toDouble()
                    ?: bindgenValue?.toString()?.toDoubleOrNull()

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
