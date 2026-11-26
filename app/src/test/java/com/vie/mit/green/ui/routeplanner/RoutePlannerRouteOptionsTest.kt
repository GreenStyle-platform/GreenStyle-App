package com.vie.mit.green.ui.routeplanner

import androidx.navigation.NavOptionsBuilder
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.common.navigation.NavigationEvent
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.remote.request.map.HereAutocompleteRequest
import com.vie.mit.data.model.remote.request.map.HereAutosuggestRequest
import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.request.map.TransitRouteSearchRequest
import com.vie.mit.data.model.remote.response.map.HereAutocompleteResponse
import com.vie.mit.data.model.remote.response.map.HereAutosuggestResponse
import com.vie.mit.data.model.remote.response.map.HereReverseGeocodeResponse
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.TransitItineraryResponse
import com.vie.mit.data.model.remote.response.map.TransitLegResponse
import com.vie.mit.data.model.remote.response.map.TransitPlaceResponse
import com.vie.mit.data.model.remote.response.map.TransitRouteSearchResponse
import com.vie.mit.data.network.api.geocode.GeocodeApi
import com.vie.mit.data.network.api.routeplanner.RoutePlannerApi
import com.vie.mit.data.repository.geocode.GeocodeRepository
import com.vie.mit.data.repository.routeplanner.RoutePlannerRepository
import com.vie.mit.green.ui.routeplanner.container.RouteOptionFilter
import com.vie.mit.green.ui.routeplanner.container.RoutePlannerContainerViewModel
import com.vie.mit.mapbox.model.LocationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePlannerRouteOptionsTest {

    @Test
    fun loadRouteOptions_loadsOtp2ItinerariesAndSupportsMultimodalFiltering() = runBlocking {
        val fakeRouteApi = FakeTransitRoutePlannerApi(
            response = TransitRouteSearchResponse(
                itineraries = listOf(
                    TransitItineraryResponse(
                        id = "otp-1",
                        durationSeconds = 5222,
                        startTime = "1795769562000",
                        endTime = "1795774784000",
                        transfers = 1,
                        walkSeconds = 2958,
                        legs = listOf(
                            TransitLegResponse(mode = "WALK", durationSeconds = 2325),
                            TransitLegResponse(
                                mode = "BUS",
                                durationSeconds = 1705,
                                routeShortName = "29",
                                from = TransitPlaceResponse(name = "Trạm A", latitude = 21.04, longitude = 105.75),
                                to = TransitPlaceResponse(name = "Trạm B", latitude = 20.99, longitude = 105.80),
                            ),
                            TransitLegResponse(mode = "WALK", durationSeconds = 459),
                            TransitLegResponse(
                                mode = "SUBWAY",
                                durationSeconds = 333,
                                routeShortName = "2A",
                                from = TransitPlaceResponse(name = "Vành Đai 3", latitude = 20.99, longitude = 105.80),
                                to = TransitPlaceResponse(name = "Văn Quán", latitude = 20.97, longitude = 105.78),
                            ),
                            TransitLegResponse(mode = "WALK", durationSeconds = 174),
                        ),
                    ),
                    TransitItineraryResponse(
                        id = "otp-3",
                        durationSeconds = 6535,
                        startTime = "1795769562000",
                        endTime = "1795776097000",
                        transfers = 0,
                        walkSeconds = 4830,
                        legs = listOf(
                            TransitLegResponse(mode = "WALK", durationSeconds = 2325),
                            TransitLegResponse(mode = "BUS", durationSeconds = 1705, routeShortName = "29"),
                            TransitLegResponse(mode = "WALK", durationSeconds = 2505),
                        ),
                    ),
                    TransitItineraryResponse(
                        id = "otp-walk",
                        durationSeconds = 3600,
                        transfers = 0,
                        walkSeconds = 3600,
                        legs = listOf(
                            TransitLegResponse(mode = "WALK", durationSeconds = 3600),
                        ),
                    ),
                ),
            ),
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val viewModel = RoutePlannerContainerViewModel(
                appNavigator = StubAppNavigator(),
                geocodeRepository = GeocodeRepository(StubGeocodeApi()),
                routePlannerRepository = RoutePlannerRepository(fakeRouteApi),
                customScope = scope,
                searchDebounceMs = 20L,
            )

            viewModel.updateStartLocation(LocationData(21.02817, 105.74122), "Xuất phát")
            viewModel.updateEndLocation(LocationData(20.97815, 105.78476), "Điểm đến")
            viewModel.loadRouteOptions()

            val loaded = viewModel.uiState.value
            assertFalse(loaded.isLoadingRoutes)
            assertNull(loaded.routeOptionsError)
            assertEquals(3, loaded.routeOptions.size)
            assertEquals("otp-1", loaded.selectedRouteOptionId)
            assertEquals("otp-1", loaded.selectedRouteOption?.id)

            // BUS matches both otp-1 (BUS + SUBWAY) and otp-3 (BUS only)
            viewModel.onSelectRouteFilter(RouteOptionFilter.BUS)
            assertEquals(TransitMode.BUS, RouteOptionFilter.BUS.transitMode)
            assertEquals(listOf("otp-1", "otp-3"), viewModel.uiState.value.filteredRouteOptions.map { it.id })
            assertEquals("otp-1", viewModel.uiState.value.selectedRouteOptionId)

            // Select otp-3 then switch to SUBWAY -> auto-selects otp-1 since otp-3 has no SUBWAY leg
            viewModel.onSelectRouteOption("otp-3")
            assertEquals("otp-3", viewModel.uiState.value.selectedRouteOptionId)

            viewModel.onSelectRouteFilter(RouteOptionFilter.SUBWAY)
            assertEquals(TransitMode.SUBWAY, RouteOptionFilter.SUBWAY.transitMode)
            assertEquals(listOf("otp-1"), viewModel.uiState.value.filteredRouteOptions.map { it.id })
            assertEquals("otp-1", viewModel.uiState.value.selectedRouteOptionId)

            // WALK matches walk-only itineraries
            viewModel.onSelectRouteFilter(RouteOptionFilter.WALK)
            assertEquals(TransitMode.WALK, RouteOptionFilter.WALK.transitMode)
            assertEquals(listOf("otp-walk"), viewModel.uiState.value.filteredRouteOptions.map { it.id })
            assertEquals("otp-walk", viewModel.uiState.value.selectedRouteOptionId)

            // Swapping locations clears stale route options
            viewModel.swapLocations()
            assertTrue(viewModel.uiState.value.routeOptions.isEmpty())
            assertNull(viewModel.uiState.value.selectedRouteOptionId)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun loadRouteOptions_setsErrorStateWhenApiFails() = runBlocking {
        val fakeRouteApi = FakeTransitRoutePlannerApi(
            error = RuntimeException("Server timeout"),
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val viewModel = RoutePlannerContainerViewModel(
                appNavigator = StubAppNavigator(),
                geocodeRepository = GeocodeRepository(StubGeocodeApi()),
                routePlannerRepository = RoutePlannerRepository(fakeRouteApi),
                customScope = scope,
                searchDebounceMs = 20L,
            )

            viewModel.updateStartLocation(LocationData(21.02817, 105.74122), "Xuất phát")
            viewModel.updateEndLocation(LocationData(20.97815, 105.78476), "Điểm đến")
            viewModel.loadRouteOptions()

            val state = viewModel.uiState.value
            assertFalse(state.isLoadingRoutes)
            assertTrue(state.routeOptions.isEmpty())
            assertNull(state.selectedRouteOptionId)
            assertTrue(!state.routeOptionsError.isNullOrBlank())
        } finally {
            scope.cancel()
        }
    }
}

private class StubAppNavigator : AppNavigator {
    private val channel = Channel<NavigationEvent>(Channel.BUFFERED)
    override val navigationChannel: ReceiveChannel<NavigationEvent> = channel
    override fun navigateTo(route: Any, builder: NavOptionsBuilder.() -> Unit) = Unit
    override fun navigateUp() = Unit
    override fun popBackStack(route: Any?, inclusive: Boolean) = Unit
}

private class StubGeocodeApi : GeocodeApi {
    override suspend fun getAutosuggest(request: HereAutosuggestRequest): HereAutosuggestResponse =
        HereAutosuggestResponse()

    override suspend fun getAutocomplete(request: HereAutocompleteRequest): HereAutocompleteResponse =
        HereAutocompleteResponse()

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        limit: Int?,
    ): HereReverseGeocodeResponse = HereReverseGeocodeResponse()
}

private class FakeTransitRoutePlannerApi(
    private val response: TransitRouteSearchResponse = TransitRouteSearchResponse(),
    private val error: Throwable? = null,
) : RoutePlannerApi {
    override suspend fun getPublicTransportSuggestions(
        location: LocationRequest,
    ): List<PublicTransportSuggestionResponse> = emptyList()

    override suspend fun searchTransitRoutes(
        request: TransitRouteSearchRequest,
    ): TransitRouteSearchResponse {
        error?.let { throw it }
        return response
    }
}
