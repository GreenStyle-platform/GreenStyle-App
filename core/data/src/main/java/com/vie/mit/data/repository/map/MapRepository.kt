package com.vie.mit.data.repository.map

import com.vie.mit.data.network.api.map.MapApi
import com.vie.mit.data.network.model.map.Location
import com.vie.mit.data.network.model.map.PublicTransportSuggestion
import com.vie.mit.data.network.util.handleApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepository @Inject constructor(
    private val mapApi: MapApi,
) {
    suspend fun getPublicTransportSuggestions(
        location: Location,
    ): Result<List<PublicTransportSuggestion>> = handleApiCall {
        mapApi.getPublicTransportSuggestions(location)
    }

    /**
     * Fetches GTFS transit stops by bounding box and zoom level.
     */
    suspend fun getTransitStopsGeoJson(
        minLat: Double,
        minLon: Double,
        maxLat: Double,
        maxLon: Double,
        zoom: Double,
        types: List<String>? = null,
    ): Result<Map<String, String>> = handleApiCall {
        try {
            mapApi.getTransitStopsGeoJson(
                minLat = minLat,
                minLon = minLon,
                maxLat = maxLat,
                maxLon = maxLon,
                zoom = zoom,
                types = types?.joinToString(","),
            )
        } catch (e: Exception) {
            // Fallback mock GeoJSON data around the center of the bounding box
            // for development/testing when backend API is not yet active.
            createMockTransitGeoJson(
                centerLat = (minLat + maxLat) / 2.0,
                centerLon = (minLon + maxLon) / 2.0
            )
        }
    }

    /**
     * Generates sample GeoJSON FeatureCollections for Bus, Metro, Train, and Bike.
     */
    private fun createMockTransitGeoJson(
        centerLat: Double,
        centerLon: Double,
    ): Map<String, String> {
        val busFeatures = (1..15).joinToString(",") { i ->
            val lat = centerLat + (i * 0.003 * if (i % 2 == 0) 1 else -1)
            val lon = centerLon + (i * 0.002 * if (i % 3 == 0) -1 else 1)
            """{
                "type": "Feature",
                "id": "bus_stop_$i",
                "geometry": { "type": "Point", "coordinates": [$lon, $lat] },
                "properties": {
                    "stop_id": "bus_$i",
                    "name": "Trạm xe buýt số $i",
                    "type": "BUS",
                    "routes": "Tuyến 01, Tuyến 08"
                }
            }"""
        }

        val metroFeatures = (1..6).joinToString(",") { i ->
            val lat = centerLat + (i * 0.006 * if (i % 2 == 0) -1 else 1)
            val lon = centerLon + (i * 0.005 * if (i % 2 == 0) 1 else -1)
            """{
                "type": "Feature",
                "id": "metro_stop_$i",
                "geometry": { "type": "Point", "coordinates": [$lon, $lat] },
                "properties": {
                    "stop_id": "metro_$i",
                    "name": "Ga Metro $i",
                    "type": "METRO",
                    "line": "Metro Tuyến 1 Bến Thành - Suối Tiên"
                }
            }"""
        }

        val trainFeatures = (1..3).joinToString(",") { i ->
            val lat = centerLat + (i * 0.012)
            val lon = centerLon + (i * 0.010)
            """{
                "type": "Feature",
                "id": "train_stop_$i",
                "geometry": { "type": "Point", "coordinates": [$lon, $lat] },
                "properties": {
                    "stop_id": "train_$i",
                    "name": "Ga đường sắt $i",
                    "type": "TRAIN",
                    "line": "Đường sắt Bắc Nam"
                }
            }"""
        }

        val bikeFeatures = (1..12).joinToString(",") { i ->
            val lat = centerLat + (i * 0.0025 * if (i % 3 == 0) 1 else -1)
            val lon = centerLon + (i * 0.0035 * if (i % 2 == 0) -1 else 1)
            """{
                "type": "Feature",
                "id": "bike_stop_$i",
                "geometry": { "type": "Point", "coordinates": [$lon, $lat] },
                "properties": {
                    "stop_id": "bike_$i",
                    "name": "Trạm xe đạp công cộng $i",
                    "type": "BIKE",
                    "available_bikes": "${(3..15).random()}"
                }
            }"""
        }

        return mapOf(
            "BUS" to """{"type": "FeatureCollection", "features": [$busFeatures]}""",
            "METRO" to """{"type": "FeatureCollection", "features": [$metroFeatures]}""",
            "TRAIN" to """{"type": "FeatureCollection", "features": [$trainFeatures]}""",
            "BIKE" to """{"type": "FeatureCollection", "features": [$bikeFeatures]}"""
        )
    }
}
