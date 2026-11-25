package com.vie.mit.green.ui.routeplanner

import androidx.navigation.NavOptionsBuilder
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.common.navigation.NavigationEvent
import com.vie.mit.data.model.domain.map.AutosuggestPlace
import com.vie.mit.data.model.remote.request.map.BusStopArrivalsRequest
import com.vie.mit.data.model.remote.request.map.HereAutocompleteRequest
import com.vie.mit.data.model.remote.request.map.HereAutosuggestRequest
import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.BusRoutePageResponse
import com.vie.mit.data.model.remote.response.map.HereAddressResponse
import com.vie.mit.data.model.remote.response.map.HereAutocompleteItemResponse
import com.vie.mit.data.model.remote.response.map.HereAutocompleteResponse
import com.vie.mit.data.model.remote.response.map.HereAutosuggestItemResponse
import com.vie.mit.data.model.remote.response.map.HereAutosuggestResponse
import com.vie.mit.data.model.remote.response.map.HerePositionResponse
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.RouteArrivalResponse
import com.vie.mit.data.model.remote.response.map.RouteTripResponse
import com.vie.mit.data.model.remote.response.map.ShapePointResponse
import com.vie.mit.data.model.remote.response.map.TransitStopDetailResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import com.vie.mit.data.model.remote.response.map.TripStopResponse
import com.vie.mit.data.network.api.map.MapApi
import com.vie.mit.data.repository.map.MapRepository
import com.vie.mit.green.ui.routeplanner.container.LocationPickerField
import com.vie.mit.green.ui.routeplanner.container.RoutePlannerContainerViewModel
import com.vie.mit.mapbox.model.LocationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePlannerContainerViewModelTest {

    @Test
    fun onQueryChanged_triggersAutosuggestAndUpdatesSuggestions() = runBlocking {
        val fakeApi = FakeMapApi(
            autosuggestHandler = {
                HereAutosuggestResponse(
                    items = listOf(
                        HereAutosuggestItemResponse(
                            id = "place_1",
                            title = "Chợ Bến Thành",
                            address = HereAddressResponse(label = "Quận 1, TP.HCM"),
                            position = HerePositionResponse(lat = 10.7725, lng = 106.6980),
                            distance = 350L,
                        ),
                    ),
                )
            },
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val viewModel = RoutePlannerContainerViewModel(
                appNavigator = FakeAppNavigator(),
                mapRepository = MapRepository(fakeApi),
                customScope = scope,
                searchDebounceMs = 20L,
            )

            viewModel.onQueryChanged(LocationPickerField.START, "Ben Thanh")
            delay(80L)

            val state = viewModel.uiState.value
            assertEquals("Ben Thanh", state.startQuery)
            assertFalse(state.isLoadingSuggestions)
            assertEquals(1, state.suggestions.size)
            assertEquals("Chợ Bến Thành", state.suggestions.first().title)
            assertEquals(1, fakeApi.autosuggestCallCount)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun onQueryChanged_fallsBackToAutocompleteWhenAutosuggestFails() = runBlocking {
        val fakeApi = FakeMapApi(
            autosuggestHandler = { throw RuntimeException("Autosuggest unavailable") },
            autocompleteHandler = {
                HereAutocompleteResponse(
                    items = listOf(
                        HereAutocompleteItemResponse(
                            id = "auto_1",
                            title = "Ga Bến Thành",
                            address = HereAddressResponse(label = "Quận 1, TP.HCM"),
                        ),
                    ),
                )
            },
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val viewModel = RoutePlannerContainerViewModel(
                appNavigator = FakeAppNavigator(),
                mapRepository = MapRepository(fakeApi),
                customScope = scope,
                searchDebounceMs = 20L,
            )

            viewModel.onQueryChanged(LocationPickerField.END, "Ga Ben")
            delay(80L)

            val state = viewModel.uiState.value
            assertEquals("Ga Ben", state.endQuery)
            assertFalse(state.isLoadingSuggestions)
            assertEquals(1, state.suggestions.size)
            assertEquals("Ga Bến Thành", state.suggestions.first().title)
            assertEquals(1, fakeApi.autosuggestCallCount)
            assertEquals(1, fakeApi.autocompleteCallCount)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun onSelectSuggestion_updatesLocationAndDoesNotRetriggerSearch() = runBlocking {
        val fakeApi = FakeMapApi(
            autosuggestHandler = {
                HereAutosuggestResponse(
                    items = listOf(
                        HereAutosuggestItemResponse(
                            id = "place_1",
                            title = "Chợ Bến Thành",
                            address = HereAddressResponse(label = "Quận 1, TP.HCM"),
                            position = HerePositionResponse(lat = 10.7725, lng = 106.6980),
                        ),
                    ),
                )
            },
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val viewModel = RoutePlannerContainerViewModel(
                appNavigator = FakeAppNavigator(),
                mapRepository = MapRepository(fakeApi),
                customScope = scope,
                searchDebounceMs = 20L,
            )

            viewModel.onQueryChanged(LocationPickerField.START, "Ben Thanh")
            delay(80L)
            assertEquals(1, fakeApi.autosuggestCallCount)

            val place = AutosuggestPlace(
                id = "place_1",
                title = "Chợ Bến Thành",
                address = "Quận 1, TP.HCM",
                latitude = 10.7725,
                longitude = 106.6980,
            )
            viewModel.onSelectSuggestion(place)
            delay(80L)

            val state = viewModel.uiState.value
            assertEquals(LocationData(10.7725, 106.6980), state.startPoint)
            assertEquals("Chợ Bến Thành", state.startAddress)
            assertEquals("Chợ Bến Thành", state.startQuery)
            assertEquals(LocationPickerField.END, state.focusedField)
            assertTrue(state.suggestions.isEmpty())
            assertEquals(1, fakeApi.autosuggestCallCount)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun confirmMapCenter_swap_clear_and_filter_keepSharedStateInSync() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val viewModel = RoutePlannerContainerViewModel(
                appNavigator = FakeAppNavigator(),
                mapRepository = MapRepository(FakeMapApi()),
                customScope = scope,
                searchDebounceMs = 20L,
            )

            // Confirm START from map center
            viewModel.onMapCameraMoved(10.7700, 106.6900)
            viewModel.onConfirmMapCenterLocation()
            assertEquals(LocationData(10.7700, 106.6900), viewModel.uiState.value.startPoint)
            assertEquals(viewModel.uiState.value.startAddress, viewModel.uiState.value.startQuery)
            assertTrue(viewModel.uiState.value.startQuery.isNotEmpty())
            assertEquals(LocationPickerField.END, viewModel.uiState.value.focusedField)

            // Confirm END from map center
            viewModel.onMapCameraMoved(10.7800, 106.7100)
            viewModel.onConfirmMapCenterLocation()
            assertEquals(LocationData(10.7800, 106.7100), viewModel.uiState.value.endPoint)
            assertEquals(viewModel.uiState.value.endAddress, viewModel.uiState.value.endQuery)
            assertTrue(viewModel.uiState.value.hasValidRoute)

            // Swap locations syncs point, address, and query
            val startBefore = viewModel.uiState.value.startAddress
            val endBefore = viewModel.uiState.value.endAddress
            viewModel.swapLocations()
            assertEquals(endBefore, viewModel.uiState.value.startAddress)
            assertEquals(endBefore, viewModel.uiState.value.startQuery)
            assertEquals(startBefore, viewModel.uiState.value.endAddress)
            assertEquals(startBefore, viewModel.uiState.value.endQuery)

            // RouteOptions filter state shared in container UiState
            viewModel.onSelectRouteFilter("Xe buýt")
            assertEquals("Xe buýt", viewModel.uiState.value.selectedRouteFilter)

            // Clear start location resets point, address, and query
            viewModel.clearStartLocation()
            assertNull(viewModel.uiState.value.startPoint)
            assertEquals("", viewModel.uiState.value.startAddress)
            assertEquals("", viewModel.uiState.value.startQuery)
            assertFalse(viewModel.uiState.value.hasValidRoute)

            // Clear end location resets point, address, and query
            viewModel.clearEndLocation()
            assertNull(viewModel.uiState.value.endPoint)
            assertEquals("", viewModel.uiState.value.endAddress)
            assertEquals("", viewModel.uiState.value.endQuery)
        } finally {
            scope.cancel()
        }
    }
}

private class FakeAppNavigator : AppNavigator {
    private val channel = Channel<NavigationEvent>(Channel.BUFFERED)
    override val navigationChannel: ReceiveChannel<NavigationEvent> = channel
    var navigatedUp: Boolean = false

    override fun navigateTo(route: Any, builder: NavOptionsBuilder.() -> Unit) = Unit
    override fun navigateUp() {
        navigatedUp = true
    }

    override fun popBackStack(route: Any?, inclusive: Boolean) = Unit
}

private class FakeMapApi(
    private val autosuggestHandler: suspend (HereAutosuggestRequest) -> HereAutosuggestResponse = {
        HereAutosuggestResponse()
    },
    private val autocompleteHandler: suspend (HereAutocompleteRequest) -> HereAutocompleteResponse = {
        HereAutocompleteResponse()
    },
) : MapApi {
    var autosuggestCallCount: Int = 0
    var autocompleteCallCount: Int = 0

    override suspend fun getAutosuggest(request: HereAutosuggestRequest): HereAutosuggestResponse {
        autosuggestCallCount++
        return autosuggestHandler(request)
    }

    override suspend fun getAutocomplete(request: HereAutocompleteRequest): HereAutocompleteResponse {
        autocompleteCallCount++
        return autocompleteHandler(request)
    }

    override suspend fun getPublicTransportSuggestions(location: LocationRequest): List<PublicTransportSuggestionResponse> =
        emptyList()

    override suspend fun searchBusStopsInBounds(bounds: TransitStopBoundsRequest): List<TransitStopMarkerResponse> =
        emptyList()

    override suspend fun searchMetroStopsInBounds(bounds: TransitStopBoundsRequest): List<TransitStopMarkerResponse> =
        emptyList()

    override suspend fun searchBikeStopsInBounds(bounds: TransitStopBoundsRequest): List<TransitStopMarkerResponse> =
        emptyList()

    override suspend fun getStopArrivals(
        mode: String,
        request: BusStopArrivalsRequest,
    ): Map<String, List<RouteArrivalResponse>> = emptyMap()

    override suspend fun getTransitStopDetail(mode: String, stopId: String): TransitStopDetailResponse =
        error("Not used in test")

    override suspend fun searchBusRoutes(query: String, page: Int): BusRoutePageResponse =
        error("Not used in test")

    override suspend fun searchSubwayRoutes(query: String, page: Int): BusRoutePageResponse =
        error("Not used in test")

    override suspend fun getRouteTrips(mode: String, routeId: String): List<RouteTripResponse> =
        emptyList()

    override suspend fun getTripShape(mode: String, tripId: String): List<ShapePointResponse> =
        emptyList()

    override suspend fun getTripStops(mode: String, tripId: String): List<TripStopResponse> =
        emptyList()
}
