package com.vie.mit.data.repository.map

import com.vie.mit.data.model.common.map.PublicTransportType
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.data.model.domain.map.Location
import com.vie.mit.data.model.domain.map.PublicTransportSuggestion
import com.vie.mit.data.model.domain.map.StopArrival
import com.vie.mit.data.model.domain.map.TransitStopsGeoJsonLoad
import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.mapper.toLocationRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import com.vie.mit.data.network.api.map.MapApi
import com.vie.mit.data.network.util.handleApiCall
import com.vie.mit.mapbox.model.TransitTypeConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepository @Inject constructor(
    private val mapApi: MapApi,
) {
    val defaultConfigs: List<TransitTypeConfig> = listOf(
        TransitTypeConfig(
            typeId = PublicTransportType.BUS.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bus,
            clusterColor = 0xFF1E88E5.toInt(),
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
        TransitTypeConfig(
            typeId = PublicTransportType.SUBWAY.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_metro,
            clusterColor = 0xFFE53935.toInt(),
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
        TransitTypeConfig(
            typeId = PublicTransportType.RAIL.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_train,
            clusterColor = 0xFF8E24AA.toInt(),
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
        TransitTypeConfig(
            typeId = TravelMode.BICYCLE_RENT.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bike,
            clusterColor = 0xFF43A047.toInt(),
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
    )

    suspend fun getPublicTransportSuggestions(
        location: Location,
    ): Result<List<PublicTransportSuggestion>> = handleApiCall {
        mapApi.getPublicTransportSuggestions(location.toLocationRequest()).map { it.toDomain() }
    }

    /**
     * Fetches scheduled arrivals for a specific bus stop.
     * Returns arrivals sorted by ETA (ascending).
     * Falls back to empty list if network/server error.
     */
    suspend fun getBusStopArrivals(stopId: String): Result<List<StopArrival>> = handleApiCall {
        mapApi.getBusStopArrivals(stopId = stopId)
            .map { it.toDomain() }
            .sortedBy { it.etaMinutes }
    }

    suspend fun getTransitStopsGeoJson(
        bounds: List<TransitStopBoundsRequest>,
        typeIdsToFetch: Set<String> = defaultConfigs.map { it.typeId }.toSet(),
    ): Result<TransitStopsGeoJsonLoad> = withContext(Dispatchers.IO) {
        try {
            require(bounds.isNotEmpty()) { "At least one viewport bound is required" }
            val configsToFetch = defaultConfigs.filter { it.typeId in typeIdsToFetch }
            val stopResults: Map<String, Result<List<TransitStopMarkerResponse>>> =
                supervisorScope {
                    configsToFetch.associate { config ->
                        config.typeId to async {
                            loadStopsResult(config.typeId, bounds)
                        }
                    }.mapValues { (_, deferredResult) -> deferredResult.await() }
                }

            Result.success(
                TransitStopsGeoJsonLoad(
                    geoJsonByType = configsToFetch.associate { config ->
                        val stops = stopResults[config.typeId]?.getOrDefault(emptyList()).orEmpty()
                        config.typeId to stops.toFeatureCollectionGeoJson(config.typeId)
                    },
                    failedTypeIds = stopResults.filterValues { it.isFailure }.keys,
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private suspend fun loadStopsResult(
        typeId: String,
        bounds: List<TransitStopBoundsRequest>,
    ): Result<List<TransitStopMarkerResponse>> = try {
        val stops: List<TransitStopMarkerResponse> = coroutineScope {
            bounds.map { bound ->
                async { loadStops(typeId, bound) }
            }.awaitAll().flatten().distinctBy(TransitStopMarkerResponse::id)
        }
        Timber.d("PhucTH: LoadStopDebug: loadStopResult called")
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
        PublicTransportType.BUS.name -> mapApi.searchBusStopsInBounds(bounds = bounds)
        PublicTransportType.SUBWAY.name -> mapApi.searchMetroStopsInBounds(bounds = bounds)
        TravelMode.BICYCLE_RENT.name -> try {
            mapApi.searchBikeStopsInBounds(bounds = bounds)
        } catch (e: Exception) {
            Timber.w(e, "searchBikeStopsInBounds endpoint failed or not yet deployed")
            emptyList()
        }
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
}
