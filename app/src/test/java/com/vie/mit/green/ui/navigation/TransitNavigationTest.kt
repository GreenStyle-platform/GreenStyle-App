package com.vie.mit.green.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavOptionsBuilder
import com.mapbox.geojson.Point
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.common.navigation.NavigationEvent
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.data.model.remote.request.map.BusStopArrivalsRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.BusRoutePageResponse
import com.vie.mit.data.model.remote.response.map.RouteArrivalResponse
import com.vie.mit.data.model.remote.response.map.RouteTripResponse
import com.vie.mit.data.model.remote.response.map.ShapePointResponse
import com.vie.mit.data.model.remote.response.map.TransitStopDetailResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import com.vie.mit.data.model.remote.response.map.TripStopResponse
import com.vie.mit.data.network.api.transit.TransitRouteApi
import com.vie.mit.data.network.api.transit.TransitStopApi
import com.vie.mit.data.repository.transit.TransitRouteRepository
import com.vie.mit.data.repository.transit.TransitStopRepository
import com.vie.mit.green.navigation.main_graph.BusRouteSearchRoute
import com.vie.mit.green.navigation.main_graph.RoutePlannerContainerRoute
import com.vie.mit.green.navigation.main_graph.SettingRoute
import com.vie.mit.green.navigation.main_graph.TransitRouteDetailRoute
import com.vie.mit.green.navigation.main_graph.TransitStopDetailRoute
import com.vie.mit.green.ui.busroutesearch.BusRouteSearchViewModel
import com.vie.mit.green.ui.home.container.HomeContainerViewModel
import com.vie.mit.green.ui.home.nearbybusstops.NearbyBusStopViewModel
import com.vie.mit.green.ui.home.nearbysubwaystations.NearbySubwayStationViewModel
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyStopItem
import com.vie.mit.green.ui.setting.SettingViewModel
import com.vie.mit.green.ui.transitroutedetail.TransitRouteDetailViewModel
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.StopFeatureClick
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitNavigationTest {

    @Test
    fun transitRouteDetail_navigatesToStopDetailWithRouteTransitMode() {
        val navigator = RecordingNavigator()
        val viewModel = TransitRouteDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("transitMode" to TransitMode.SUBWAY)),
            transitRouteRepository = TransitRouteRepository(StubTransitRouteApi()),
            appNavigator = navigator,
        )

        viewModel.navigateToTransitStopDetail("   ")
        assertTrue(navigator.navigatedRoutes.isEmpty())

        viewModel.navigateToTransitStopDetail("stop_metro_1")
        assertEquals(
            listOf(TransitStopDetailRoute(stopId = "stop_metro_1", transitMode = TransitMode.SUBWAY)),
            navigator.navigatedRoutes,
        )

        viewModel.navigateBack()
        assertEquals(1, navigator.navigateUpCount)
    }

    @Test
    fun nearbyBusStop_navigatesToBothStopDetailAndRouteDetail() {
        val navigator = RecordingNavigator()
        val viewModel = NearbyBusStopViewModel(
            appNavigator = navigator,
            transitStopRepository = TransitStopRepository(StubTransitStopApi()),
        )

        viewModel.navigateToTransitStopDetail("")
        viewModel.navigateToTransitRouteDetail(" ")
        assertTrue(navigator.navigatedRoutes.isEmpty())

        viewModel.navigateToTransitStopDetail("bus_stop_1")
        viewModel.navigateToTransitRouteDetail("bus_route_36")

        assertEquals(
            listOf(
                TransitStopDetailRoute(stopId = "bus_stop_1", transitMode = TransitMode.BUS),
                TransitRouteDetailRoute(routeId = "bus_route_36", transitMode = TransitMode.BUS),
            ),
            navigator.navigatedRoutes,
        )
    }

    @Test
    fun nearbySubwayStation_navigatesToStopAndRouteDetail_andIgnoresBlankIds() {
        val navigator = RecordingNavigator()
        val viewModel = NearbySubwayStationViewModel(
            appNavigator = navigator,
            transitRouteRepository = TransitRouteRepository(StubTransitRouteApi()),
        )

        viewModel.navigateToRouteDetail("")
        viewModel.navigateToTransitStopDetail("")
        assertTrue(navigator.navigatedRoutes.isEmpty())

        viewModel.navigateToTransitStopDetail("metro_station_1")
        viewModel.navigateToRouteDetail("metro_line_1")

        assertEquals(
            listOf(
                TransitStopDetailRoute(stopId = "metro_station_1", transitMode = TransitMode.SUBWAY),
                TransitRouteDetailRoute(routeId = "metro_line_1", transitMode = TransitMode.SUBWAY),
            ),
            navigator.navigatedRoutes,
        )
    }

    @Test
    fun busRouteSearch_navigatesToBusRouteDetail_andBack() {
        val navigator = RecordingNavigator()
        val viewModel = BusRouteSearchViewModel(
            appNavigator = navigator,
            transitRouteRepository = TransitRouteRepository(StubTransitRouteApi()),
        )

        viewModel.navigateToDetailRoute("")
        assertTrue(navigator.navigatedRoutes.isEmpty())

        viewModel.navigateToDetailRoute("route_08B")
        assertEquals(
            listOf(TransitRouteDetailRoute(routeId = "route_08B", transitMode = TransitMode.BUS)),
            navigator.navigatedRoutes,
        )

        viewModel.navigateBack()
        assertEquals(1, navigator.navigateUpCount)
    }

    @Test
    fun homeContainer_canNavigateToStopDetailMatchesSupportedTransitModesOnly() {
        val navigator = RecordingNavigator()
        val viewModel = HomeContainerViewModel(
            transitStopRepository = TransitStopRepository(StubTransitStopApi()),
            appNavigator = navigator,
        )

        assertTrue(viewModel.canNavigateToStopDetail(TransitMode.BUS.name))
        assertTrue(viewModel.canNavigateToStopDetail(TransitMode.SUBWAY.name))
        assertTrue(viewModel.canNavigateToStopDetail("METRO"))
        assertTrue(viewModel.canNavigateToStopDetail(TransitMode.RAIL.name))
        assertTrue(viewModel.canNavigateToStopDetail("TRAIN"))

        assertFalse(viewModel.canNavigateToStopDetail(TravelMode.BICYCLE_RENT.name))
        assertFalse(viewModel.canNavigateToStopDetail(TravelMode.BICYCLE.name))
        assertFalse(viewModel.canNavigateToStopDetail("BIKE"))
        assertFalse(viewModel.canNavigateToStopDetail("UNKNOWN"))
    }

    @Test
    fun homeContainer_sheetChangeClearsSelectedStop_andBikeSelectionFocusesMap() {
        val navigator = RecordingNavigator()
        val viewModel = HomeContainerViewModel(
            transitStopRepository = TransitStopRepository(StubTransitStopApi()),
            appNavigator = navigator,
        )

        val bikeStop = NearbyStopItem(
            stopId = "bike_1",
            name = "Trạm xe đạp Bến Thành",
            typeId = TravelMode.BICYCLE_RENT.name,
            location = LocationData(10.7725, 106.6980),
            distanceMeters = 120f,
            routesOrInfo = "",
            properties = emptyMap(),
        )
        viewModel.onNearbyStopSelected(bikeStop)

        assertTrue(navigator.navigatedRoutes.isEmpty())
        assertEquals("bike_1", viewModel.uiState.value.selectedStop?.stopId)

        viewModel.onActiveSheetChanged(isNearbySheet = false, transitTypeId = null)
        assertNull(viewModel.uiState.value.selectedStop)

        val railStop = NearbyStopItem(
            stopId = "rail_1",
            name = "Ga Sài Gòn",
            typeId = TransitMode.RAIL.name,
            location = LocationData(10.7825, 106.6770),
            distanceMeters = 450f,
            routesOrInfo = "",
            properties = emptyMap(),
        )
        viewModel.onNearbyStopSelected(railStop)
        viewModel.navigateToSelectedStopDetail(
            StopFeatureClick(
                stopId = "bus_2",
                typeId = TransitMode.BUS.name,
                name = "Trạm Bến Thành",
                point = Point.fromLngLat(106.6980, 10.7725),
                properties = emptyMap(),
            )
        )
        viewModel.navigateToSettingScreen()
        viewModel.navigateToBusRouteSearch()
        viewModel.navigateToRouteLocationPicker()

        assertEquals(
            listOf(
                TransitStopDetailRoute(stopId = "rail_1", transitMode = TransitMode.RAIL),
                TransitStopDetailRoute(stopId = "bus_2", transitMode = TransitMode.BUS),
                SettingRoute,
                BusRouteSearchRoute,
                RoutePlannerContainerRoute,
            ),
            navigator.navigatedRoutes,
        )
    }

    @Test
    fun settingViewModel_navigatesBack() {
        val navigator = RecordingNavigator()
        val viewModel = SettingViewModel(appNavigator = navigator)

        viewModel.navigateBack()
        assertEquals(1, navigator.navigateUpCount)
    }
}

private class RecordingNavigator : AppNavigator {
    private val channel = Channel<NavigationEvent>(Channel.BUFFERED)
    override val navigationChannel: ReceiveChannel<NavigationEvent> = channel
    val navigatedRoutes = mutableListOf<Any>()
    var navigateUpCount: Int = 0

    override fun navigateTo(route: Any, builder: NavOptionsBuilder.() -> Unit) {
        navigatedRoutes.add(route)
    }

    override fun navigateUp() {
        navigateUpCount++
    }

    override fun popBackStack(route: Any?, inclusive: Boolean) = Unit
}

private class StubTransitStopApi : TransitStopApi {
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
}

private class StubTransitRouteApi : TransitRouteApi {
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
