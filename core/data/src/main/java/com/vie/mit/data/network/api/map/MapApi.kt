package com.vie.mit.data.network.api.map

import com.vie.mit.data.model.remote.request.map.BusStopArrivalsRequest
import com.vie.mit.data.model.remote.request.map.HereAutocompleteRequest
import com.vie.mit.data.model.remote.request.map.HereAutosuggestRequest
import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.BusRoutePageResponse
import com.vie.mit.data.model.remote.response.map.HereAutocompleteResponse
import com.vie.mit.data.model.remote.response.map.HereAutosuggestResponse
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.RouteArrivalResponse
import com.vie.mit.data.model.remote.response.map.RouteTripResponse
import com.vie.mit.data.model.remote.response.map.ShapePointResponse
import com.vie.mit.data.model.remote.response.map.TransitStopDetailResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import com.vie.mit.data.model.remote.response.map.TripStopResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MapApi {
    @POST("map/public-transports")
    suspend fun getPublicTransportSuggestions(
        @Body location: LocationRequest,
    ): List<PublicTransportSuggestionResponse>

    @POST("here-geocode/autosuggest")
    suspend fun getAutosuggest(
        @Body request: HereAutosuggestRequest,
    ): HereAutosuggestResponse

    @POST("here-geocode/autocomplete")
    suspend fun getAutocomplete(
        @Body request: HereAutocompleteRequest,
    ): HereAutocompleteResponse

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
     *
     * NOTE: This endpoint is not yet available on the backend.
     * Currently [MapRepository] uses [getStopArrivals] as a fallback.
     */
    @GET("transits/{mode}/search-stop/{stopId}/routes")
    suspend fun getTransitStopDetail(
        @Path("mode") mode: String = "bus", @Path("stopId") stopId: String
    ): TransitStopDetailResponse

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
