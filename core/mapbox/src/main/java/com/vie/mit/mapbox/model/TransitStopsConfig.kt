package com.vie.mit.mapbox.model

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.mapbox.geojson.Point

/**
 * Visual configuration for a specific public transit type (e.g., Bus, Metro, Train, Bike).
 *
 * @param typeId Unique identifier for this transit type (e.g., "BUS", "METRO", "TRAIN", "BICYCLE").
 * @param iconResId Android drawable resource ID representing the unclustered stop marker icon.
 * @param iconName Unique identifier string registered into Mapbox Style image cache.
 * @param clusterColor Primary color of the cluster circle bubble.
 * @param clusterTextColor Color of the number of stops text inside the cluster bubble.
 * @param iconSize Scaling factor for the symbol icon on the map.
 */
data class TransitTypeConfig(
    val typeId: String,
    @param:DrawableRes val iconResId: Int,
    val iconName: String = "transit_icon_$typeId",
    @param:ColorInt val clusterColor: Int,
    @param:ColorInt val clusterTextColor: Int = 0xFFFFFFFF.toInt(),
    val iconSize: Double = 1.0,
)

/**
 * GeoJSON source data for a specific transit type.
 *
 * @param typeId Type identifier matching a [TransitTypeConfig.typeId].
 * @param geoJson Raw GeoJSON FeatureCollection string.
 * @param isVisible Whether this transit layer should be visible on the map.
 */
data class TransitSourceData(
    val typeId: String,
    val geoJson: String,
    val isVisible: Boolean = true,
)

/**
 * Clustering configurations for transit sources.
 *
 * @param clusterRadius Radius of each cluster if clustering is enabled. A cluster will include all points within this radius.
 * @param clusterMaxZoom Max zoom level to cluster points on. Cluster points will break into individual points above this zoom.
 * @param minZoomToDisplay Minimum zoom level for stops to begin loading/displaying on the map.
 */
data class TransitClusterConfig(
    val clusterRadius: Long = 24L,
    val clusterMaxZoom: Long = 14L,
    val minZoomToDisplay: Double = 12.0,
)

/**
 * Event payload when an unclustered transit stop symbol is clicked.
 *
 * @param stopId Unique ID of the stop (from feature id or "id" / "stop_id" property).
 * @param typeId Type identifier (e.g. "BUS").
 * @param name Name of the stop (if present in properties).
 * @param point Geographic location of the stop.
 * @param properties All raw properties from the GeoJSON feature.
 */
data class StopFeatureClick(
    val stopId: String,
    val typeId: String,
    val name: String?,
    val point: Point,
    val properties: Map<String, String>,
)

/**
 * Geographic bounding box and zoom level of the camera viewport.
 * Useful for triggering data load from ViewModel without reloading on zoom-in.
 */
data class TransitMapViewportInfo(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double,
    val zoom: Double,
    val centerLatitude: Double,
    val centerLongitude: Double,
)

