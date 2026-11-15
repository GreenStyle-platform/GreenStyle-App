package com.vie.mit.data.repository.map

import com.vie.mit.data.network.api.map.MapApi
import com.vie.mit.data.network.model.map.Location
import com.vie.mit.data.network.model.map.PublicTransportSuggestion
import com.vie.mit.data.network.model.map.TransitStopBoundsRequest
import com.vie.mit.data.network.model.map.TransitStopMarkerResponse
import com.vie.mit.data.network.util.handleApiCall
import com.vie.mit.mapbox.model.TransitTypeConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepository @Inject constructor(
    private val mapApi: MapApi,
) {
    val defaultConfigs: List<TransitTypeConfig> = listOf(
        TransitTypeConfig(
            typeId = "BUS",
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bus,
            clusterColor = 0xFF1E88E5.toInt(),
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
        TransitTypeConfig(
            typeId = "METRO",
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_metro,
            clusterColor = 0xFFE53935.toInt(),
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
        TransitTypeConfig(
            typeId = "TRAIN",
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_train,
            clusterColor = 0xFF8E24AA.toInt(),
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
        TransitTypeConfig(
            typeId = "BIKE",
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bike,
            clusterColor = 0xFF43A047.toInt(),
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
    )

    suspend fun getPublicTransportSuggestions(
        location: Location,
    ): Result<List<PublicTransportSuggestion>> = handleApiCall {
        mapApi.getPublicTransportSuggestions(location)
    }

    suspend fun getTransitStopsGeoJson(
        minLat: Double,
        minLon: Double,
        maxLat: Double,
        maxLon: Double,
    ): Result<TransitStopsGeoJsonLoad> = getTransitStopsGeoJson(
        listOf(TransitStopBoundsRequest(minLat, minLon, maxLat, maxLon)),
    )

    suspend fun getTransitStopsGeoJson(
        bounds: List<TransitStopBoundsRequest>,
    ): Result<TransitStopsGeoJsonLoad> = try {
        require(bounds.isNotEmpty()) { "At least one viewport bound is required" }
        val stopResults = supervisorScope {
            defaultConfigs.associate { config ->
                config.typeId to async {
                    loadStopsResult(config.typeId, bounds)
                }
            }.mapValues { (_, deferredResult) -> deferredResult.await() }
        }

        val supportedResults = stopResults.filterKeys { it in SUPPORTED_TRANSIT_TYPES }
        if (supportedResults.isNotEmpty() && supportedResults.values.all { it.isFailure }) {
            throw supportedResults.values.first().exceptionOrNull()!!
        }

        Result.success(
            TransitStopsGeoJsonLoad(
                geoJsonByType = defaultConfigs.associate { config ->
                val stops = stopResults[config.typeId]?.getOrDefault(emptyList()).orEmpty()
                config.typeId to stops.toFeatureCollectionGeoJson(config.typeId)
                },
                failedTypeIds = supportedResults.filterValues { it.isFailure }.keys,
            ),
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    private suspend fun loadStopsResult(
        typeId: String,
        bounds: List<TransitStopBoundsRequest>,
    ): Result<List<TransitStopMarkerResponse>> = try {
        val stops = coroutineScope {
            bounds.map { bound ->
                async { loadStops(typeId, bound) }
            }.awaitAll().flatten().distinctBy(TransitStopMarkerResponse::id)
        }
        Result.success(stops)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    private suspend fun loadStops(
        typeId: String,
        bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse> = when (typeId) {
        "BUS" -> mapApi.searchBusStopsInBounds(bounds)
        "METRO" -> mapApi.searchMetroStopsInBounds(bounds)
        else -> emptyList()
    }

    private fun List<TransitStopMarkerResponse>.toFeatureCollectionGeoJson(typeId: String): String =
        buildJsonObject {
            put("type", "FeatureCollection")
            putJsonArray("features") {
                this@toFeatureCollectionGeoJson.forEach { stop ->
                    addJsonObject {
                        put("type", "Feature")
                        put("id", stop.id)
                        putJsonObject("geometry") {
                            put("type", "Point")
                            putJsonArray("coordinates") {
                                add(stop.longitude)
                                add(stop.latitude)
                            }
                        }
                        putJsonObject("properties") {
                            put("stop_id", stop.id)
                            put("name", stop.name)
                            put("type", typeId)
                        }
                    }
                }
            }
        }.toString()

    private companion object {
        val SUPPORTED_TRANSIT_TYPES = setOf("BUS", "METRO")
    }
}
