package com.vie.mit.data.network.api.transit

import com.vie.mit.data.model.remote.response.map.BusRoutePageResponse
import com.vie.mit.data.model.remote.response.map.RouteTripResponse
import com.vie.mit.data.model.remote.response.map.ShapePointResponse
import com.vie.mit.data.model.remote.response.map.TripStopResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TransitRouteApi {
    /**
     * Search bus routes by query string or route number.
     */
    @GET("transits/bus/search-routes")
    suspend fun searchBusRoutes(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
    ): BusRoutePageResponse

    /**
     * Search subway (metro) routes by query string or route number.
     */
    @GET("transits/subway/search-routes")
    suspend fun searchSubwayRoutes(
        @Query("query") query: String = "",
        @Query("page") page: Int = 1,
    ): BusRoutePageResponse

    /**
     * Get GTFS trips of a route.
     *
     * Example: GET transits/bus/search-trip-route?routeId=r12997260
     */
    @GET("transits/{mode}/search-trip-route")
    suspend fun getRouteTrips(
        @Path("mode") mode: String = "bus",
        @Query("routeId") routeId: String,
    ): List<RouteTripResponse>

    /**
     * Get list of shape coordinate points for a specific trip.
     *
     * Example: GET transits/bus/r12921723_Mo%2CTu%2CWe%2CTh_050000/search-shape
     */
    @GET("transits/{mode}/{tripId}/search-shape")
    suspend fun getTripShape(
        @Path("mode") mode: String = "bus",
        @Path("tripId") tripId: String,
    ): List<ShapePointResponse>

    /**
     * Get list of transit stops along a specific trip.
     *
     * Example: GET transits/bus/trips/r12921723_Fr_050000/search-stops
     */
    @GET("transits/{mode}/trips/{tripId}/search-stops")
    suspend fun getTripStops(
        @Path("mode") mode: String = "bus",
        @Path("tripId") tripId: String,
    ): List<TripStopResponse>
}
