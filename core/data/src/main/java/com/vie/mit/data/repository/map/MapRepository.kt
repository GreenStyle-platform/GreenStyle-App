package com.vie.mit.data.repository.map

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.data.model.common.map.toTransitTypeConfig
import com.vie.mit.data.model.domain.map.AutosuggestPlace
import com.vie.mit.data.model.domain.map.BusRoute
import com.vie.mit.data.model.domain.map.Location
import com.vie.mit.data.model.domain.map.PublicTransportSuggestion
import com.vie.mit.data.model.domain.map.RouteOption
import com.vie.mit.data.model.domain.map.RouteOptionTransportType
import com.vie.mit.data.model.domain.map.RouteTrip
import com.vie.mit.data.model.domain.map.ShapePoint
import com.vie.mit.data.model.domain.map.StopArrival
import com.vie.mit.data.model.domain.map.TransitStopDetail
import com.vie.mit.data.model.domain.map.TransitStopMarker
import com.vie.mit.data.model.domain.map.TransitStopsGeoJsonLoad
import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.mapper.toLocationRequest
import com.vie.mit.data.model.remote.request.map.BusStopArrivalsRequest
import com.vie.mit.data.model.remote.request.map.HereAutocompleteRequest
import com.vie.mit.data.model.remote.request.map.HereAutosuggestRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.BusRoutePageResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import com.vie.mit.data.network.api.map.MapApi
import com.vie.mit.data.network.util.handleApiCall
import com.vie.mit.data.paging.BusRoutePagingSource
import com.vie.mit.mapbox.model.TransitTypeConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MapRepository(
    private val mapApi: MapApi,
) {
    private var appContext: Context? = null

    @Inject
    constructor(
        mapApi: MapApi,
        @ApplicationContext context: Context,
    ) : this(mapApi) {
        this.appContext = context.applicationContext
    }
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

    suspend fun getPublicTransportSuggestions(
        location: Location,
    ): Result<List<PublicTransportSuggestion>> = handleApiCall {
        mapApi.getPublicTransportSuggestions(location.toLocationRequest()).map { it.toDomain() }
    }

    /**
     * Searches places and locations using HERE Autosuggest API (returns coordinates).
     */
    suspend fun getAutosuggest(
        query: String,
        latitude: Double,
        longitude: Double,
    ): Result<List<AutosuggestPlace>> = handleApiCall {
        mapApi.getAutosuggest(
            HereAutosuggestRequest(
                query = query,
                latitude = latitude,
                longitude = longitude,
            )
        ).items.map { it.toDomain() }
    }

    /**
     * Suggests keywords and text completions using HERE Autocomplete API.
     */
    suspend fun getAutocomplete(
        query: String,
        latitude: Double,
        longitude: Double,
    ): Result<List<AutosuggestPlace>> = handleApiCall {
        mapApi.getAutocomplete(
            HereAutocompleteRequest(
                query = query,
                latitudeIn = latitude,
                longitudeIn = longitude,
            )
        ).items.map { it.toDomain() }
    }

    /**
     * Reverse geocodes map coordinates into a human-readable address label using Android [Geocoder]
     * with a graceful coordinate fallback when unavailable or offline.
     *
     * TODO: Prefer backend HERE reverse-geocode endpoint when available.
     */
    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): Result<String> = withContext(Dispatchers.IO) {
        val fallback = "Vị trí đã chọn (${
            String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
        })"
        val context = appContext
        if (context == null || !Geocoder.isPresent()) {
            return@withContext Result.success(fallback)
        }

        try {
            val geocoder = Geocoder(context, Locale.forLanguageTag("vi-VN"))
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<Address?> { continuation ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) {
                                    continuation.resume(addresses.firstOrNull())
                                }
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        },
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }

            val label = address?.toDisplayLabel()
            Result.success(if (!label.isNullOrBlank()) label else fallback)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Reverse geocode failed for ($latitude, $longitude), using coordinate fallback")
            Result.success(fallback)
        }
    }

    private fun Address.toDisplayLabel(): String? {
        val line = getAddressLine(0)?.trim()
        if (!line.isNullOrBlank()) return line
        val parts = listOfNotNull(
            featureName?.takeIf { it.isNotBlank() },
            thoroughfare?.takeIf { it.isNotBlank() },
            subLocality?.takeIf { it.isNotBlank() },
            locality?.takeIf { it.isNotBlank() },
            adminArea?.takeIf { it.isNotBlank() },
        ).distinct()
        return parts.joinToString(", ").takeIf { it.isNotBlank() }
    }

    /**
     * Fetches multimodal route options between Start and End coordinates.
     *
     * TODO: Replace mock implementation with real backend route-planning API when available.
     */
    suspend fun getRouteOptions(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
    ): Result<List<RouteOption>> {
        val distanceKm = estimateDistanceKm(startLat, startLng, endLat, endLng)
        val busMinutes = (distanceKm * 4.2 + 8).toInt().coerceIn(10, 120)
        val metroMinutes = (distanceKm * 2.5 + 6).toInt().coerceIn(8, 90)
        val bikeMinutes = (distanceKm * 4.0 + 4).toInt().coerceIn(6, 120)
        val walkMinutes = (distanceKm * 12.0).toInt().coerceIn(5, 180)
        val walkMeters = (distanceKm * 1000).toInt().coerceAtLeast(150)

        return Result.success(
            listOf(
                RouteOption(
                    id = "opt_bus_fast",
                    title = "Xe buýt tuyến 01",
                    description = "Đi bộ 250m • Chuyến kế tiếp trong 5 phút",
                    durationMinutes = busMinutes,
                    costVnd = 7000,
                    walkDistanceMeters = 250,
                    transportType = RouteOptionTransportType.BUS,
                    tags = listOf("Nhanh nhất", "Tiết kiệm"),
                ),
                RouteOption(
                    id = "opt_metro_1",
                    title = "Tàu điện Metro Tuyến 1",
                    description = "Chuyển tuyến 1 lần • Giãn cách 8 phút",
                    durationMinutes = metroMinutes,
                    costVnd = 12000,
                    walkDistanceMeters = 400,
                    transportType = RouteOptionTransportType.SUBWAY,
                    tags = listOf("Đúng giờ", "Tránh kẹt xe"),
                ),
                RouteOption(
                    id = "opt_bike",
                    title = "Xe đạp công cộng TNGo",
                    description = "Thuê tại trạm gần nhất • Tuyến đường thoáng",
                    durationMinutes = bikeMinutes,
                    costVnd = 10000,
                    walkDistanceMeters = 100,
                    transportType = RouteOptionTransportType.BIKE,
                    tags = listOf("Thân thiện môi trường"),
                ),
                RouteOption(
                    id = "opt_walk",
                    title = "Đi bộ",
                    description = "Tuyến đường đi bộ an toàn có vỉa hè",
                    durationMinutes = walkMinutes,
                    costVnd = 0,
                    walkDistanceMeters = walkMeters,
                    transportType = RouteOptionTransportType.WALK,
                    tags = listOf("Tập luyện"),
                ),
            ),
        )
    }

    private fun estimateDistanceKm(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val sinLat = kotlin.math.sin(dLat / 2)
        val sinLng = kotlin.math.sin(dLng / 2)
        val a = sinLat * sinLat +
            kotlin.math.cos(Math.toRadians(lat1)) *
            kotlin.math.cos(Math.toRadians(lat2)) *
            sinLng * sinLng
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (earthRadiusKm * c).coerceAtLeast(0.5)
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
    ): Result<List<StopArrival>> = if (stopId.isEmpty()) {
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
     * Calls [mapApi.getTransitStopDetail] to retrieve stop details including coordinates,
     * name, and routes with arrival schedules. Falls back to [getStopArrivals] if the
     * detail endpoint is unreachable.
     *
     * @param stopId The unique ID of the transit stop.
     * @param type Transit type: BUS, SUBWAY, RAIL, etc.
     * @return A [Result] containing a [TransitStopDetail] with coordinates, stop info, and route arrivals.
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
        }

        try {
            mapApi.getTransitStopDetail(
                mode = mode,
                stopId = stopId,
            ).toDomain()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(
                e,
                "getTransitStopDetail endpoint failed, falling back to getStopArrivals for stopId: $stopId"
            )
            val arrivalsMap = mapApi.getStopArrivals(
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

    /**
     * Searches subway (metro) routes by query string.
     */
    suspend fun searchSubwayRoutes(
        query: String = "",
        page: Int = 1,
    ): Result<BusRoutePageResponse> = handleApiCall {
        mapApi.searchSubwayRoutes(query = query, page = page)
    }

    /**
     * Fetches all subway (metro) routes matching a query (or all if empty) as a list of [BusRoute].
     */
    suspend fun getSubwayRoutes(
        query: String = "",
        page: Int = 1,
    ): Result<List<BusRoute>> = handleApiCall {
        mapApi.searchSubwayRoutes(query = query, page = page).content.map { it.toDomain() }
    }

    /**
     * Fetches GTFS trips for a route.
     *
     * @param mode Transit mode (default: "bus").
     * @param routeId Route identifier (e.g. "r12997260").
     */
    suspend fun getRouteTrips(
        mode: String = "bus",
        routeId: String,
    ): Result<List<RouteTrip>> = handleApiCall {
        mapApi.getRouteTrips(mode = mode, routeId = routeId).map { it.toDomain() }
    }

    /**
     * Fetches shape points for a specific trip.
     *
     * @param mode Transit mode (default: "bus").
     * @param tripId Trip identifier.
     */
    suspend fun getTripShape(
        mode: String = "bus",
        tripId: String,
    ): Result<List<ShapePoint>> = handleApiCall {
        mapApi.getTripShape(mode = mode, tripId = tripId).mapNotNull { it.toDomain() }
    }

    /**
     * Fetches transit stops for a specific trip.
     *
     * @param mode Transit mode (default: "bus").
     * @param tripId Trip identifier.
     */
    suspend fun getTripStops(
        mode: String = "bus",
        tripId: String,
    ): Result<List<TransitStopMarker>> = handleApiCall {
        mapApi.getTripStops(mode = mode, tripId = tripId).map { it.toDomain() }
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
        TransitMode.BUS.name -> mapApi.searchBusStopsInBounds(bounds = bounds)
        TransitMode.SUBWAY.name -> mapApi.searchMetroStopsInBounds(bounds = bounds)
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
