package com.vie.mit.data.network.api.map

import com.vie.mit.data.model.remote.request.map.BusStopArrivalsRequest
import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.BusRoutePageResponse
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.RouteArrivalResponse
import com.vie.mit.data.model.remote.response.map.TransitStopDetailResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MapApi {
    @POST("map/public-transports")
    suspend fun getPublicTransportSuggestions(
        @Body location: LocationRequest,
    ): List<PublicTransportSuggestionResponse>

    @POST("transits/bus/search-in-bounds")
    suspend fun searchBusStopsInBounds(
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>

    @POST("transits/subway/search-in-bounds")
    suspend fun searchMetroStopsInBounds(
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>

    @POST("bike-rental-stations/search-in-bounds")
    suspend fun searchBikeStopsInBounds(
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>

    /**
     * Fetch scheduled arrivals for transit stops by stop IDs.
     * Returns a Map where key is stopId and value is list of route arrivals.
     */
    @POST("transits/{mode}/search-by-ids")
    suspend fun getStopArrivals(
        @Path("mode") mode: String = "bus",
        @Body request: BusStopArrivalsRequest,
    ): Map<String, List<RouteArrivalResponse>>

    /**
     * Fetch detailed information for a specific transit stop by its ID.
     * Returns stop info along with routes passing through and their scheduled arrivals.
     *
     * NOTE: This endpoint is not yet available on the backend.
     * Currently [MapRepository] uses [getStopArrivals] as a fallback.
     */
    @GET("transits/{mode}/search-stop/{stopId}/routes")
    suspend fun getTransitStopDetail(
        @Path("mode") mode: String = "bus", @Path("stopId") stopId: String
    ): TransitStopDetailResponse

    /**
     * Search bus routes by query string or route number.
     */
    @GET("transits/bus/search-routes")
    suspend fun searchBusRoutes(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
    ): BusRoutePageResponse
}
