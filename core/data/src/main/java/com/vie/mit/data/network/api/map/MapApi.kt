package com.vie.mit.data.network.api.map

import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.StopArrivalResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MapApi {
    @POST("map/public-transports")
    suspend fun getPublicTransportSuggestions(
        @Body location: LocationRequest,
    ): List<PublicTransportSuggestionResponse>

    @POST("transit-stops/bus/search-in-bounds")
    suspend fun searchBusStopsInBounds(
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>

    @POST("transit-stops/metro/search-in-bounds")
    suspend fun searchMetroStopsInBounds(
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>

    @POST("transit-stops/bike/search-in-bounds")
    suspend fun searchBikeStopsInBounds(
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>

    /**
     * Fetch scheduled arrivals for a specific bus stop.
     * Returns list of route arrivals sorted by ETA ascending.
     */
    @GET("transit-stops/bus/{stopId}/arrivals")
    suspend fun getBusStopArrivals(
        @Path("stopId") stopId: String,
    ): List<StopArrivalResponse>
}
