package com.vie.mit.data.repository.map

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.vie.mit.data.model.common.map.PublicTransportType
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.data.model.domain.map.BusRoute
import com.vie.mit.data.model.domain.map.Location
import com.vie.mit.data.model.domain.map.PublicTransportSuggestion
import com.vie.mit.data.model.domain.map.StopArrival
import com.vie.mit.data.model.domain.map.TransitStopDetail
import com.vie.mit.data.model.domain.map.TransitStopsGeoJsonLoad
import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.mapper.toLocationRequest
import com.vie.mit.data.model.remote.request.map.BusStopArrivalsRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import com.vie.mit.data.network.api.map.MapApi
import com.vie.mit.data.network.util.handleApiCall
import com.vie.mit.data.paging.BusRoutePagingSource
import com.vie.mit.mapbox.model.TransitTypeConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
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
     * Fetches scheduled route arrivals for multiple bus stops.
     * Falls back to empty list if network/server error.
     */
    suspend fun getBusStopArrivals(stopIds: List<String>): Result<List<StopArrival>> =
        if (stopIds.isEmpty()) {
            Result.success(emptyList())
        } else {
            Timber.d("PhucTH: getBusStopArrivals: $stopIds")
            handleApiCall {
                mapApi.getStopArrivals(
                    mode = "bus",
                    request = BusStopArrivalsRequest(stopIds = stopIds),
                ).toDomain()
            }
        }

    /**
     * Fetches scheduled route arrivals for a specific transit stop with mode support.
     * Uses [getStopArrivals] API which works for all transit modes.
     *
     * @param stopId The unique ID of the transit stop.
     * @param mode Transit mode: "bus", "subway", "rail", etc.
     * @return A [Result] containing a list of [StopArrival] for the stop.
     */
    suspend fun getStopArrivalsForMode(
        stopId: String,
        mode: String = "bus",
    ): Result<List<StopArrival>> =
        if (stopId.isEmpty()) {
            Result.success(emptyList())
        } else {
            handleApiCall {
                mapApi.getStopArrivals(
                    mode = mode,
                    request = BusStopArrivalsRequest(stopIds = listOf(stopId)),
                ).toDomain()
            }
        }

    /**
     * Fetches detailed information for a specific transit stop.
     *
     * Currently uses [getStopArrivals] API as the backend stop-detail endpoint
     * is not yet available. When the backend is ready, switch to [mapApi.getTransitStopDetail].
     *
     * @param stopId The unique ID of the transit stop.
     * @param mode Transit mode: "bus", "subway", "rail", etc.
     * @return A [Result] containing a [TransitStopDetail] with route arrivals.
     */
    suspend fun getTransitStopDetail(
        stopId: String,
        mode: String = "bus",
    ): Result<TransitStopDetail> = handleApiCall {
        // TODO: Replace with direct API call when backend stop-detail endpoint is ready:
        // mapApi.getTransitStopDetail(
        //     mode = mode,
        //     request = TransitStopDetailRequest(stopId = stopId),
        // ).toDomain()

        // Temporary: use existing getStopArrivals to get route/arrival data
        val arrivalsMap = mapApi.getStopArrivals(
            mode = mode,
            request = BusStopArrivalsRequest(stopIds = listOf(stopId)),
        )
        val arrivals = arrivalsMap.toDomain()

        TransitStopDetail(
            id = stopId,
            name = "",       // Populated from navigation args in ViewModel
            latitude = 0.0,  // Populated from navigation args in ViewModel
            longitude = 0.0, // Populated from navigation args in ViewModel
            type = mode.uppercase(),
            routeArrivals = arrivals,
        )
    }

    /**
     * Searches bus routes by query keyword or route number with Paging 3 pagination.
     *
     * @param query Search query string (e.g. "01", "Bến Thành")
     * @return A [Flow] of [PagingData] containing matching [BusRoute] domain objects.
     */
    fun getBusRoutesStream(query: String): Flow<PagingData<BusRoute>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { BusRoutePagingSource(mapApi, query) },
        ).flow
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
