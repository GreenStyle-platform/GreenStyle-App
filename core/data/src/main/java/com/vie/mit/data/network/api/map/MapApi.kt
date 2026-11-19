package com.vie.mit.data.network.api.map

import com.vie.mit.data.model.remote.request.map.BusStopArrivalsRequest
import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.StopArrivalResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface MapApi {
    @POST("map/public-transports")
    suspend fun getPublicTransportSuggestions(
        @Body location: LocationRequest,
    ): List<PublicTransportSuggestionResponse>

    @POST("transit-trips/bus/search-in-bounds")
    suspend fun searchBusStopsInBounds(
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>

    @POST("transit-trips/subway/search-in-bounds")
    suspend fun searchMetroStopsInBounds(
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>

    @POST("bike-rental-stations/search-in-bounds")
    suspend fun searchBikeStopsInBounds(
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>

    /**
     * Fetch scheduled arrivals for bus stops.
     * Returns list of route arrivals with upcoming arrival times.
     */
    @POST("transit-stops/bus/arrivals")
    suspend fun getBusStopArrivals(
        @Body request: BusStopArrivalsRequest,
    ): List<StopArrivalResponse>
}
