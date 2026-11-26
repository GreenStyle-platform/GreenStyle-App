package com.vie.mit.data.repository.transit

import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.data.model.common.map.toTransitTypeConfig
import com.vie.mit.data.model.domain.map.StopArrival
import com.vie.mit.data.model.domain.map.TransitStopDetail
import com.vie.mit.data.model.domain.map.TransitStopsGeoJsonLoad
import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.remote.request.map.BusStopArrivalsRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import com.vie.mit.data.network.api.transit.TransitStopApi
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
class TransitStopRepository @Inject constructor(
    private val transitStopApi: TransitStopApi,
) {
    val defaultConfigs: List<TransitTypeConfig> = listOf(
        TransitMode.BUS.toTransitTypeConfig(iconSize = 1.0),
        TransitMode.SUBWAY.toTransitTypeConfig(iconSize = 1.0),
        TransitMode.RAIL.toTransitTypeConfig(iconSize = 1.0),
        TransitTypeConfig(
            typeId = TravelMode.BICYCLE_RENT.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bike,
            clusterColor = 0xFF43A047.toInt(),
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
    )

    /**
     * Fetches scheduled route arrivals for multiple bus stops.
     * Falls back to empty list if network/server error.
     */
    suspend fun getBusStopArrivals(stopIds: List<String>): Result<List<StopArrival>> =
        if (stopIds.isEmpty()) {
            Result.success(emptyList())
        } else {
            Timber.d("PhucTH: getBusStopArrivals: $stopIds")
            handleApiCall {
                transitStopApi.getStopArrivals(
                    mode = "bus",
                    request = BusStopArrivalsRequest(stopIds = stopIds),
                ).toDomain()
            }
        }

    /**
     * Fetches scheduled route arrivals for a specific transit stop with mode support.
     * Uses [TransitStopApi.getStopArrivals] which works for all transit modes.
     *
     * @param stopId The unique ID of the transit stop.
     * @param mode Transit mode: "bus", "subway", "rail", etc.
     * @return A [Result] containing a list of [StopArrival] for the stop.
     */
    suspend fun getStopArrivalsForMode(
        stopId: String,
        mode: String = "bus",
    ): Result<List<StopArrival>> = if (stopId.isEmpty()) {
        Result.success(emptyList())
    } else {
        handleApiCall {
            transitStopApi.getStopArrivals(
                mode = mode,
                request = BusStopArrivalsRequest(stopIds = listOf(stopId)),
            ).toDomain()
        }
    }

    /**
     * Fetches detailed information for a specific transit stop.
     *
     * Calls [TransitStopApi.getTransitStopDetail] to retrieve stop details including coordinates,
     * name, and routes with arrival schedules. Falls back to [TransitStopApi.getStopArrivals]
     * if the detail endpoint is unreachable.
     */
    suspend fun getTransitStopDetail(
        stopId: String,
        type: TransitMode = TransitMode.BUS,
    ): Result<TransitStopDetail> = handleApiCall {
        val mode = when (type) {
            TransitMode.BUS, TransitMode.TROLLEYBUS -> "bus"
            TransitMode.SUBWAY, TransitMode.MONORAIL -> "subway"
            TransitMode.RAIL, TransitMode.TRAM, TransitMode.CABLE_TRAM, TransitMode.AERIAL_LIFT -> "rail"
            TransitMode.FERRY -> "ferry"
            TransitMode.BIKE, TransitMode.WALK -> "bus"
        }

        try {
            transitStopApi.getTransitStopDetail(
                mode = mode,
                stopId = stopId,
            ).toDomain()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(
                e,
                "getTransitStopDetail endpoint failed, falling back to getStopArrivals for stopId: $stopId",
            )
            val arrivalsMap = transitStopApi.getStopArrivals(
                mode = mode,
                request = BusStopArrivalsRequest(stopIds = listOf(stopId)),
            )
            TransitStopDetail(
                id = stopId,
                name = "",
                latitude = 0.0,
                longitude = 0.0,
                type = type.name,
                routeArrivals = arrivalsMap.toDomain(),
            )
        }
    }

    suspend fun getTransitStopDetail(
        stopId: String,
        mode: String,
    ): Result<TransitStopDetail> {
        val type = when (mode.lowercase()) {
            "subway", "metro" -> TransitMode.SUBWAY
            "rail", "train" -> TransitMode.RAIL
            "tram" -> TransitMode.TRAM
            "ferry" -> TransitMode.FERRY
            else -> TransitMode.BUS
        }
        return getTransitStopDetail(stopId = stopId, type = type)
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
        TransitMode.BUS.name -> transitStopApi.searchBusStopsInBounds(bounds = bounds)
        TransitMode.SUBWAY.name -> transitStopApi.searchMetroStopsInBounds(bounds = bounds)
        TravelMode.BICYCLE_RENT.name -> try {
            transitStopApi.searchBikeStopsInBounds(bounds = bounds)
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
