package com.vie.mit.data.network.api.map

import com.vie.mit.data.network.model.map.Location
import com.vie.mit.data.network.model.map.PublicTransportSuggestion
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface MapApi {
    @POST("map/public-transports")
    suspend fun getPublicTransportSuggestions(
        @Body location: Location,
    ): List<PublicTransportSuggestion>

    /**
     * Retrieves GTFS transit stops grouped by transit type in GeoJSON format.
     *
     * @param minLat South latitude
     * @param minLon West longitude
     * @param maxLat North latitude
     * @param maxLon East longitude
     * @param zoom Current camera zoom level
     * @param types Optional comma-separated filter for types (e.g. "BUS,METRO,TRAIN,BIKE")
     * @return Map of transit type ID to raw GeoJSON FeatureCollection string
     */
    @GET("map/gtfs/stops")
    suspend fun getTransitStopsGeoJson(
        @Query("minLat") minLat: Double,
        @Query("minLon") minLon: Double,
        @Query("maxLat") maxLat: Double,
        @Query("maxLon") maxLon: Double,
        @Query("zoom") zoom: Double,
        @Query("types") types: String? = null,
    ): Map<String, String>
}
