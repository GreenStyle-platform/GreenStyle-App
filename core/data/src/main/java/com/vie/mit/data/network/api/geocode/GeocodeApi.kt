package com.vie.mit.data.network.api.geocode

import com.vie.mit.data.model.remote.request.map.HereAutocompleteRequest
import com.vie.mit.data.model.remote.request.map.HereAutosuggestRequest
import com.vie.mit.data.model.remote.response.map.HereAutocompleteResponse
import com.vie.mit.data.model.remote.response.map.HereAutosuggestResponse
import com.vie.mit.data.model.remote.response.map.HereReverseGeocodeResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GeocodeApi {
    @POST("here-geocode/autosuggest")
    suspend fun getAutosuggest(
        @Body request: HereAutosuggestRequest,
    ): HereAutosuggestResponse

    @POST("here-geocode/autocomplete")
    suspend fun getAutocomplete(
        @Body request: HereAutocompleteRequest,
    ): HereAutocompleteResponse

    @GET("here-geocode/reverse-geocode")
    suspend fun reverseGeocode(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("limit") limit: Int? = null,
    ): HereReverseGeocodeResponse
}
