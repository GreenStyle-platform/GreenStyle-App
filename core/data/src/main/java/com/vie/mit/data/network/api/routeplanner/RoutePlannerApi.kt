package com.vie.mit.data.network.api.routeplanner

import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.request.map.TransitRouteSearchRequest
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.TransitRouteSearchResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface RoutePlannerApi {
    @POST("map/public-transports")
    suspend fun getPublicTransportSuggestions(
        @Body location: LocationRequest,
    ): List<PublicTransportSuggestionResponse>

    @POST("transit-routes/search")
    suspend fun searchTransitRoutes(
        @Body request: TransitRouteSearchRequest,
    ): TransitRouteSearchResponse
}
