package com.vie.mit.data.network.api.transit

import com.vie.mit.data.model.remote.request.map.BusStopArrivalsRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.RouteArrivalResponse
import com.vie.mit.data.model.remote.response.map.TransitStopDetailResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TransitStopApi {
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
     */
    @GET("transits/{mode}/search-stop/{stopId}/routes")
    suspend fun getTransitStopDetail(
        @Path("mode") mode: String = "bus",
        @Path("stopId") stopId: String,
    ): TransitStopDetailResponse
}
