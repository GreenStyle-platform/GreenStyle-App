package com.vie.mit.data.repository.transit

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.vie.mit.data.model.domain.map.BusRoute
import com.vie.mit.data.model.domain.map.RouteTrip
import com.vie.mit.data.model.domain.map.ShapePoint
import com.vie.mit.data.model.domain.map.TransitStopMarker
import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.remote.response.map.BusRoutePageResponse
import com.vie.mit.data.network.api.transit.TransitRouteApi
import com.vie.mit.data.network.util.handleApiCall
import com.vie.mit.data.paging.BusRoutePagingSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransitRouteRepository @Inject constructor(
    private val transitRouteApi: TransitRouteApi,
) {
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
            pagingSourceFactory = { BusRoutePagingSource(transitRouteApi, query) },
        ).flow
    }

    /**
     * Searches subway (metro) routes by query string.
     */
    suspend fun searchSubwayRoutes(
        query: String = "",
        page: Int = 1,
    ): Result<BusRoutePageResponse> = handleApiCall {
        transitRouteApi.searchSubwayRoutes(query = query, page = page)
    }

    /**
     * Fetches all subway (metro) routes matching a query (or all if empty) as a list of [BusRoute].
     */
    suspend fun getSubwayRoutes(
        query: String = "",
        page: Int = 1,
    ): Result<List<BusRoute>> = handleApiCall {
        transitRouteApi.searchSubwayRoutes(query = query, page = page).content.map { it.toDomain() }
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
        transitRouteApi.getRouteTrips(mode = mode, routeId = routeId).map { it.toDomain() }
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
        transitRouteApi.getTripShape(mode = mode, tripId = tripId).mapNotNull { it.toDomain() }
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
        transitRouteApi.getTripStops(mode = mode, tripId = tripId).map { it.toDomain() }
    }
}
