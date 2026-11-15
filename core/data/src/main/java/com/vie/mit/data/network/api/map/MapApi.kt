package com.vie.mit.data.network.api.map

import com.vie.mit.data.network.model.map.Location
import com.vie.mit.data.network.model.map.PublicTransportSuggestion
import com.vie.mit.data.network.model.map.TransitStopBoundsRequest
import com.vie.mit.data.network.model.map.TransitStopMarkerResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface MapApi {
    @POST("map/public-transports")
    suspend fun getPublicTransportSuggestions(
        @Body location: Location,
    ): List<PublicTransportSuggestion>

    @POST("transit-stops/bus/search-in-bounds")
    suspend fun searchBusStopsInBounds(
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>

    @POST("transit-stops/metro/search-in-bounds")
    suspend fun searchMetroStopsInBounds(
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>
}
