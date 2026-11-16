package com.vie.mit.data.network.api.map

import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MapApi {
    @POST("map/public-transports")
    suspend fun getPublicTransportSuggestions(
        @Body location: LocationRequest,
    ): List<PublicTransportSuggestionResponse>

    @POST("transit-stops/bus/search-in-bounds")
    suspend fun searchBusStopsInBounds(
        @Header("Authorization") token: String = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwaHVjQGdtYWlsLmNvbSIsInNjb3BlIjpbIkRSSVZFUiJdLCJpc3MiOiJoYXBpZ29fc2VydmVyIiwiaXNQaG9uZVZlcmlmeSI6ZmFsc2UsImV4cCI6MTc5NDkyNjI4MiwiaWF0IjoxNzk0ODM5ODgyLCJqdGkiOiI1MDY5MjA2ZS05ZjBlLTQ1ZmItYWRkNS02Y2Y3NTgwNTkzOGQiLCJpc0VtYWlsVmVyaWZ5IjpmYWxzZX0.DQgfTTHxZu_3Z0ixHJzekBeFihoZDDpm6qOsM6OWv0PfZJsSwBapEdHyirrhE_xkB4FRVPdVZ2uVy7P_5C9XzQ",
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>

    @POST("transit-stops/metro/search-in-bounds")
    suspend fun searchMetroStopsInBounds(
        @Header("Authorization") token: String = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwaHVjQGdtYWlsLmNvbSIsInNjb3BlIjpbIkRSSVZFUiJdLCJpc3MiOiJoYXBpZ29fc2VydmVyIiwiaXNQaG9uZVZlcmlmeSI6ZmFsc2UsImV4cCI6MTc5NDkyNjI4MiwiaWF0IjoxNzk0ODM5ODgyLCJqdGkiOiI1MDY5MjA2ZS05ZjBlLTQ1ZmItYWRkNS02Y2Y3NTgwNTkzOGQiLCJpc0VtYWlsVmVyaWZ5IjpmYWxzZX0.DQgfTTHxZu_3Z0ixHJzekBeFihoZDDpm6qOsM6OWv0PfZJsSwBapEdHyirrhE_xkB4FRVPdVZ2uVy7P_5C9XzQ",
        @Body bounds: TransitStopBoundsRequest,
    ): List<TransitStopMarkerResponse>
}
