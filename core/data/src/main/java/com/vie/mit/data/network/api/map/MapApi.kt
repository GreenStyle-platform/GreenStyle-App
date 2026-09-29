package com.vie.mit.data.network.api.map

import com.vie.mit.data.network.model.map.Location
import com.vie.mit.data.network.model.map.PublicTransportSuggestion
import retrofit2.http.Body
import retrofit2.http.POST

interface MapApi {
    @POST("map/public-transports")
    suspend fun getPublicTransportSuggestions(
        @Body location: Location,
    ): List<PublicTransportSuggestion>
}
