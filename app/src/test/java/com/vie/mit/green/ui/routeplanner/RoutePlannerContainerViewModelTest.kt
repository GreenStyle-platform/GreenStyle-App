//package com.vie.mit.green.ui.routeplanner
//
//import androidx.navigation.NavOptionsBuilder
//import com.vie.mit.common.navigation.AppNavigator
//import com.vie.mit.common.navigation.NavigationEvent
//import com.vie.mit.data.model.common.map.TransitMode
//import com.vie.mit.data.model.domain.map.AutosuggestPlace
//import com.vie.mit.data.model.domain.map.RouteOptionTransportType
//import com.vie.mit.data.model.remote.request.map.HereAutocompleteRequest
//import com.vie.mit.data.model.remote.request.map.HereAutosuggestRequest
//import com.vie.mit.data.model.remote.request.map.LocationRequest
//import com.vie.mit.data.model.remote.response.map.HereAddressResponse
//import com.vie.mit.data.model.remote.response.map.HereAutocompleteItemResponse
//import com.vie.mit.data.model.remote.response.map.HereAutocompleteResponse
//import com.vie.mit.data.model.remote.response.map.HereAutosuggestItemResponse
//import com.vie.mit.data.model.remote.response.map.HereAutosuggestResponse
//import com.vie.mit.data.model.remote.response.map.HereCategoryResponse
//import com.vie.mit.data.model.remote.response.map.HerePositionResponse
//import com.vie.mit.data.model.remote.response.map.HereReverseGeocodeItemResponse
//import com.vie.mit.data.model.remote.response.map.HereReverseGeocodeResponse
//import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
//import com.vie.mit.data.network.api.geocode.GeocodeApi
//import com.vie.mit.data.network.api.routeplanner.RoutePlannerApi
//import com.vie.mit.data.repository.geocode.GeocodeRepository
//import com.vie.mit.data.repository.routeplanner.RoutePlannerRepository
//import com.vie.mit.green.ui.routeplanner.container.LocationPickerField
//import com.vie.mit.green.ui.routeplanner.container.RouteOptionFilter
//import com.vie.mit.green.ui.routeplanner.container.RoutePlannerContainerViewModel
//import com.vie.mit.mapbox.model.LocationData
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.channels.Channel
//import kotlinx.coroutines.channels.ReceiveChannel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.runBlocking
//import org.junit.Assert.assertEquals
//import org.junit.Assert.assertFalse
//import org.junit.Assert.assertNull
//import org.junit.Assert.assertTrue
//import org.junit.Test
//
//class RoutePlannerContainerViewModelTest {
//
//    @Test
//    fun onQueryChanged_triggersAutosuggestAndUpdatesSuggestions() = runBlocking {
//        val fakeApi = FakeGeocodeApi(
//            autosuggestHandler = {
//                HereAutosuggestResponse(
//                    items = listOf(
//                        HereAutosuggestItemResponse(
//                            id = "place_1",
//                            title = "Chợ Bến Thành",
//                            address = HereAddressResponse(label = "Quận 1, TP.HCM"),
//                            position = HerePositionResponse(lat = 10.7725, lng = 106.6980),
//                            distance = 350L,
//                        ),
//                    ),
//                )
//            },
//        )
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(fakeApi),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//            )
//
//            viewModel.onQueryChanged(LocationPickerField.START, "Ben Thanh")
//            delay(80L)
//
//            val state = viewModel.uiState.value
//            assertEquals("Ben Thanh", state.startQuery)
//            assertFalse(state.isLoadingSuggestions)
//            assertEquals(1, state.suggestions.size)
//            assertEquals("Chợ Bến Thành", state.suggestions.first().title)
//            assertEquals(1, fakeApi.autosuggestCallCount)
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun onQueryChanged_fallsBackToAutocompleteWhenAutosuggestFails() = runBlocking {
//        val fakeApi = FakeGeocodeApi(
//            autosuggestHandler = { throw RuntimeException("Autosuggest unavailable") },
//            autocompleteHandler = {
//                HereAutocompleteResponse(
//                    items = listOf(
//                        HereAutocompleteItemResponse(
//                            id = "auto_1",
//                            title = "Ga Bến Thành",
//                            address = HereAddressResponse(label = "Quận 1, TP.HCM"),
//                        ),
//                    ),
//                )
//            },
//        )
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(fakeApi),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//            )
//
//            viewModel.onQueryChanged(LocationPickerField.END, "Ga Ben")
//            delay(80L)
//
//            val state = viewModel.uiState.value
//            assertEquals("Ga Ben", state.endQuery)
//            assertFalse(state.isLoadingSuggestions)
//            assertEquals(1, state.suggestions.size)
//            assertEquals("Ga Bến Thành", state.suggestions.first().title)
//            assertEquals(1, fakeApi.autosuggestCallCount)
//            assertEquals(1, fakeApi.autocompleteCallCount)
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun onSelectSuggestion_updatesLocationAndDoesNotRetriggerSearch() = runBlocking {
//        val fakeApi = FakeGeocodeApi(
//            autosuggestHandler = {
//                HereAutosuggestResponse(
//                    items = listOf(
//                        HereAutosuggestItemResponse(
//                            id = "place_1",
//                            title = "Chợ Bến Thành",
//                            address = HereAddressResponse(label = "Quận 1, TP.HCM"),
//                            position = HerePositionResponse(lat = 10.7725, lng = 106.6980),
//                        ),
//                    ),
//                )
//            },
//        )
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(fakeApi),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//            )
//
//            viewModel.onQueryChanged(LocationPickerField.START, "Ben Thanh")
//            delay(80L)
//            assertEquals(1, fakeApi.autosuggestCallCount)
//
//            val place = AutosuggestPlace(
//                id = "place_1",
//                title = "Chợ Bến Thành",
//                address = "Quận 1, TP.HCM",
//                latitude = 10.7725,
//                longitude = 106.6980,
//            )
//            viewModel.onSelectSuggestion(place)
//            delay(80L)
//
//            val state = viewModel.uiState.value
//            assertEquals(LocationData(10.7725, 106.6980), state.startPoint)
//            assertEquals("Chợ Bến Thành", state.startAddress)
//            assertEquals("Chợ Bến Thành", state.startQuery)
//            assertEquals(LocationPickerField.END, state.focusedField)
//            assertTrue(state.suggestions.isEmpty())
//            assertEquals(1, fakeApi.autosuggestCallCount)
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun confirmMapCenter_swap_clear_and_filter_keepSharedStateInSync() = runBlocking {
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(FakeGeocodeApi()),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//            )
//
//            // Confirm START from map center
//            viewModel.onMapCameraMoved(10.7700, 106.6900)
//            viewModel.onConfirmMapCenterLocation()
//            assertEquals(LocationData(10.7700, 106.6900), viewModel.uiState.value.startPoint)
//            assertEquals(viewModel.uiState.value.startAddress, viewModel.uiState.value.startQuery)
//            assertTrue(viewModel.uiState.value.startQuery.isNotEmpty())
//            assertEquals(LocationPickerField.END, viewModel.uiState.value.focusedField)
//
//            // Confirm END from map center
//            viewModel.onMapCameraMoved(10.7800, 106.7100)
//            viewModel.onConfirmMapCenterLocation()
//            assertEquals(LocationData(10.7800, 106.7100), viewModel.uiState.value.endPoint)
//            assertEquals(viewModel.uiState.value.endAddress, viewModel.uiState.value.endQuery)
//            assertTrue(viewModel.uiState.value.hasValidRoute)
//
//            // Swap locations syncs point, address, and query
//            val startBefore = viewModel.uiState.value.startAddress
//            val endBefore = viewModel.uiState.value.endAddress
//            viewModel.swapLocations()
//            assertEquals(endBefore, viewModel.uiState.value.startAddress)
//            assertEquals(endBefore, viewModel.uiState.value.startQuery)
//            assertEquals(startBefore, viewModel.uiState.value.endAddress)
//            assertEquals(startBefore, viewModel.uiState.value.endQuery)
//
//            // RouteOptions filter state shared in container UiState
//            viewModel.onSelectRouteFilter(RouteOptionFilter.BUS)
//            assertEquals(RouteOptionFilter.BUS, viewModel.uiState.value.selectedRouteFilter)
//
//            // Clear start location resets point, address, and query
//            viewModel.clearStartLocation()
//            assertNull(viewModel.uiState.value.startPoint)
//            assertEquals("", viewModel.uiState.value.startAddress)
//            assertEquals("", viewModel.uiState.value.startQuery)
//            assertFalse(viewModel.uiState.value.hasValidRoute)
//
//            // Clear end location resets point, address, and query
//            viewModel.clearEndLocation()
//            assertNull(viewModel.uiState.value.endPoint)
//            assertEquals("", viewModel.uiState.value.endAddress)
//            assertEquals("", viewModel.uiState.value.endQuery)
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun onLocationUpdated_initialFix_autoFillsStartAndFocusesEnd() = runBlocking {
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(FakeGeocodeApi()),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//            )
//
//            val gpsLocation = LocationData(latitude = 10.7755, longitude = 106.7011)
//            viewModel.onLocationUpdated(gpsLocation)
//
//            val state = viewModel.uiState.value
//            assertEquals(gpsLocation, state.currentLocation)
//            assertEquals(gpsLocation, state.startPoint)
//            assertEquals(RoutePlannerContainerViewModel.CURRENT_LOCATION_LABEL, state.startAddress)
//            assertEquals(RoutePlannerContainerViewModel.CURRENT_LOCATION_LABEL, state.startQuery)
//            assertEquals(LocationPickerField.END, state.focusedField)
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun loadRouteOptions_returnsMockOptionsAndSelectsFirst() = runBlocking {
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(FakeGeocodeApi()),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//            )
//
//            viewModel.updateStartLocation(LocationData(10.7725, 106.6980), "Chợ Bến Thành")
//            viewModel.updateEndLocation(LocationData(10.7769, 106.7009), "Nhà thờ Đức Bà")
//            viewModel.loadRouteOptions()
//
//            val state = viewModel.uiState.value
//            assertFalse(state.isLoadingRoutes)
//            assertNull(state.routeOptionsError)
//            assertTrue(state.routeOptions.isNotEmpty())
//            assertEquals(state.routeOptions.first().id, state.selectedRouteOptionId)
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun onSelectRouteFilter_filtersRouteOptionsByEnumAndMapsTransitMode() = runBlocking {
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(FakeGeocodeApi()),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//            )
//
//            viewModel.updateStartLocation(LocationData(10.7725, 106.6980), "Chợ Bến Thành")
//            viewModel.updateEndLocation(LocationData(10.7769, 106.7009), "Nhà thờ Đức Bà")
//            viewModel.loadRouteOptions()
//
//            // Default ALL returns all loaded options
//            assertEquals(RouteOptionFilter.ALL, viewModel.uiState.value.selectedRouteFilter)
//            assertEquals(viewModel.uiState.value.routeOptions.size, viewModel.uiState.value.filteredRouteOptions.size)
//
//            // BUS filter
//            viewModel.onSelectRouteFilter(RouteOptionFilter.BUS)
//            assertEquals(TransitMode.BUS, RouteOptionFilter.BUS.transitMode)
//            assertTrue(viewModel.uiState.value.filteredRouteOptions.isNotEmpty())
//            assertTrue(viewModel.uiState.value.filteredRouteOptions.all { it.transportType == RouteOptionTransportType.BUS })
//
//            // SUBWAY filter
//            viewModel.onSelectRouteFilter(RouteOptionFilter.SUBWAY)
//            assertEquals(TransitMode.SUBWAY, RouteOptionFilter.SUBWAY.transitMode)
//            assertTrue(viewModel.uiState.value.filteredRouteOptions.isNotEmpty())
//            assertTrue(viewModel.uiState.value.filteredRouteOptions.all { it.transportType == RouteOptionTransportType.SUBWAY })
//
//            // BIKE filter
//            viewModel.onSelectRouteFilter(RouteOptionFilter.BIKE)
//            assertNull(RouteOptionFilter.BIKE.transitMode)
//            assertTrue(viewModel.uiState.value.filteredRouteOptions.isNotEmpty())
//            assertTrue(viewModel.uiState.value.filteredRouteOptions.all { it.transportType == RouteOptionTransportType.BIKE })
//
//            // WALK filter
//            viewModel.onSelectRouteFilter(RouteOptionFilter.WALK)
//            assertNull(RouteOptionFilter.WALK.transitMode)
//            assertTrue(viewModel.uiState.value.filteredRouteOptions.isNotEmpty())
//            assertTrue(viewModel.uiState.value.filteredRouteOptions.all { it.transportType == RouteOptionTransportType.WALK })
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun mapDrag_whenStartFocused_updatesStartLocationLiveAndKeepsStartFocus() = runBlocking {
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(FakeGeocodeApi()),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//                reverseGeocodeDebounceMs = 30L,
//            )
//
//            viewModel.setFocusedField(LocationPickerField.START)
//            viewModel.onMapMoveStateChanged(true)
//            viewModel.onMapCameraMoved(10.77123, 106.69876)
//
//            val draggingState = viewModel.uiState.value
//            assertTrue(draggingState.isMapMoving)
//            assertEquals(LocationData(10.77123, 106.69876), draggingState.startPoint)
//            assertEquals("10.77123, 106.69876", draggingState.startQuery)
//            assertEquals(LocationPickerField.START, draggingState.focusedField)
//
//            viewModel.onMapMoveStateChanged(false)
//            viewModel.onMapCameraIdle(10.77123, 106.69876)
//            delay(80L)
//
//            val settledState = viewModel.uiState.value
//            assertFalse(settledState.isMapMoving)
//            assertEquals(LocationData(10.77123, 106.69876), settledState.startPoint)
//            assertEquals("Vị trí đã chọn (10.7712, 106.6988)", settledState.startAddress)
//            assertEquals(settledState.startAddress, settledState.startQuery)
//            assertEquals(LocationPickerField.START, settledState.focusedField)
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun mapDrag_whenEndFocused_resolvesAfterMoveEndDebounceEvenWithoutIdle() = runBlocking {
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(FakeGeocodeApi()),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//                reverseGeocodeDebounceMs = 30L,
//            )
//
//            viewModel.updateStartLocation(LocationData(10.7725, 106.6980), "Chợ Bến Thành")
//            assertEquals(LocationPickerField.END, viewModel.uiState.value.focusedField)
//
//            viewModel.onMapMoveStateChanged(true)
//            viewModel.onMapCameraMoved(10.78500, 106.70500)
//            viewModel.onMapMoveStateChanged(false)
//            delay(100L)
//
//            val state = viewModel.uiState.value
//            assertEquals(LocationData(10.78500, 106.70500), state.endPoint)
//            assertEquals("Vị trí đã chọn (10.7850, 106.7050)", state.endAddress)
//            assertEquals(state.endAddress, state.endQuery)
//            assertTrue(state.hasValidRoute)
//            assertEquals(LocationPickerField.END, state.focusedField)
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun programmaticCameraMove_doesNotOverwriteSelectedLocation() = runBlocking {
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(FakeGeocodeApi()),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//                reverseGeocodeDebounceMs = 20L,
//            )
//
//            val expectedStart = LocationData(10.7725, 106.6980)
//            viewModel.updateStartLocation(expectedStart, "Chợ Bến Thành")
//            viewModel.setFocusedField(LocationPickerField.START)
//
//            // Camera moves without user drag gesture (e.g., flyTo or bottom sheet padding)
//            viewModel.onMapCameraMoved(10.8000, 106.7500)
//            viewModel.onMapCameraIdle(10.8000, 106.7500)
//            delay(80L)
//
//            val state = viewModel.uiState.value
//            assertEquals(LocationData(10.8000, 106.7500), state.mapCenter)
//            assertEquals(expectedStart, state.startPoint)
//            assertEquals("Chợ Bến Thành", state.startAddress)
//            assertEquals("Chợ Bến Thành", state.startQuery)
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun mapDrag_flingAfterMoveEnd_usesFinalIdleCoordinates() = runBlocking {
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(FakeGeocodeApi()),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//                reverseGeocodeDebounceMs = 80L,
//            )
//
//            viewModel.setFocusedField(LocationPickerField.START)
//            viewModel.onMapMoveStateChanged(true)
//            viewModel.onMapCameraMoved(10.7700, 106.6900)
//            viewModel.onMapMoveStateChanged(false)
//            // Fling continues after finger lifts
//            viewModel.onMapCameraMoved(10.7750, 106.6950)
//            viewModel.onMapCameraIdle(10.7780, 106.6980)
//            delay(80L)
//
//            val state = viewModel.uiState.value
//            assertEquals(LocationData(10.7780, 106.6980), state.startPoint)
//            assertEquals("Vị trí đã chọn (10.7780, 106.6980)", state.startAddress)
//            assertEquals(state.startAddress, state.startQuery)
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun mapDrag_afterTypingQuery_usesTypedQueryHintToSelectBestReverseGeocodeResult() = runBlocking {
//        val fakeApi = FakeGeocodeApi(
//            reverseGeocodeHandler = { _, _, _ ->
//                HereReverseGeocodeResponse(
//                    items = listOf(
//                        HereReverseGeocodeItemResponse(
//                            title = "Nội Thất HT",
//                            resultType = "place",
//                            address = HereAddressResponse(
//                                label = "Nội Thất HT, Ngách 56/26 Lê Quang Đạo, Hà Nội",
//                            ),
//                            distance = 59L,
//                            categories = listOf(
//                                HereCategoryResponse(
//                                    name = "Phụ kiện nội thất và gia đình",
//                                    primary = true,
//                                ),
//                            ),
//                        ),
//                        HereReverseGeocodeItemResponse(
//                            title = "Duy Linh Quán",
//                            resultType = "place",
//                            address = HereAddressResponse(
//                                label = "Duy Linh Quán, Đường Châu Văn Liêm, Hà Nội",
//                            ),
//                            distance = 112L,
//                            categories = listOf(
//                                HereCategoryResponse(
//                                    name = "Nhà hàng",
//                                    primary = true,
//                                ),
//                            ),
//                        ),
//                    ),
//                )
//            },
//        )
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(fakeApi),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//                reverseGeocodeDebounceMs = 20L,
//            )
//
//            viewModel.onQueryChanged(LocationPickerField.END, "nha hang")
//            viewModel.onMapMoveStateChanged(true)
//            viewModel.onMapCameraMoved(21.00971, 105.76938)
//            viewModel.onMapMoveStateChanged(false)
//            viewModel.onMapCameraIdle(21.00971, 105.76938)
//            delay(80L)
//
//            val state = viewModel.uiState.value
//            assertEquals(LocationData(21.00971, 105.76938), state.endPoint)
//            assertEquals("Duy Linh Quán, Đường Châu Văn Liêm, Hà Nội", state.endAddress)
//            assertEquals("Duy Linh Quán, Đường Châu Văn Liêm, Hà Nội", state.endQuery)
//            assertEquals(20, fakeApi.lastReverseGeocodeLimit)
//        } finally {
//            scope.cancel()
//        }
//    }
//
//    @Test
//    fun mapDrag_withoutTypedQuery_usesClosestReverseGeocodeResultAndSmallerLimit() = runBlocking {
//        val fakeApi = FakeGeocodeApi(
//            reverseGeocodeHandler = { _, _, _ ->
//                HereReverseGeocodeResponse(
//                    items = listOf(
//                        HereReverseGeocodeItemResponse(
//                            title = "Nội Thất HT",
//                            resultType = "place",
//                            address = HereAddressResponse(
//                                label = "Nội Thất HT, Ngách 56/26 Lê Quang Đạo, Hà Nội",
//                            ),
//                            distance = 59L,
//                        ),
//                        HereReverseGeocodeItemResponse(
//                            title = "Duy Linh Quán",
//                            resultType = "place",
//                            address = HereAddressResponse(
//                                label = "Duy Linh Quán, Đường Châu Văn Liêm, Hà Nội",
//                            ),
//                            distance = 112L,
//                        ),
//                    ),
//                )
//            },
//        )
//        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
//        try {
//            val viewModel = RoutePlannerContainerViewModel(
//                appNavigator = FakeAppNavigator(),
//                geocodeRepository = GeocodeRepository(fakeApi),
//                routePlannerRepository = RoutePlannerRepository(FakeRoutePlannerApi()),
//                customScope = scope,
//                searchDebounceMs = 20L,
//                reverseGeocodeDebounceMs = 20L,
//            )
//
//            viewModel.setFocusedField(LocationPickerField.START)
//            viewModel.onMapMoveStateChanged(true)
//            viewModel.onMapCameraMoved(21.00971, 105.76938)
//            viewModel.onMapMoveStateChanged(false)
//            viewModel.onMapCameraIdle(21.00971, 105.76938)
//            delay(80L)
//
//            val state = viewModel.uiState.value
//            assertEquals("Nội Thất HT, Ngách 56/26 Lê Quang Đạo, Hà Nội", state.startAddress)
//            assertEquals("Nội Thất HT, Ngách 56/26 Lê Quang Đạo, Hà Nội", state.startQuery)
//            assertEquals(5, fakeApi.lastReverseGeocodeLimit)
//        } finally {
//            scope.cancel()
//        }
//    }
//}
//
//private class FakeAppNavigator : AppNavigator {
//    private val channel = Channel<NavigationEvent>(Channel.BUFFERED)
//    override val navigationChannel: ReceiveChannel<NavigationEvent> = channel
//    var navigatedUp: Boolean = false
//
//    override fun navigateTo(route: Any, builder: NavOptionsBuilder.() -> Unit) = Unit
//    override fun navigateUp() {
//        navigatedUp = true
//    }
//
//    override fun popBackStack(route: Any?, inclusive: Boolean) = Unit
//}
//
//private class FakeGeocodeApi(
//    private val autosuggestHandler: suspend (HereAutosuggestRequest) -> HereAutosuggestResponse = {
//        HereAutosuggestResponse()
//    },
//    private val autocompleteHandler: suspend (HereAutocompleteRequest) -> HereAutocompleteResponse = {
//        HereAutocompleteResponse()
//    },
//    private val reverseGeocodeHandler: suspend (Double, Double, Int?) -> HereReverseGeocodeResponse = { _, _, _ ->
//        HereReverseGeocodeResponse()
//    },
//) : GeocodeApi {
//    var autosuggestCallCount: Int = 0
//    var autocompleteCallCount: Int = 0
//    var reverseGeocodeCallCount: Int = 0
//    var lastReverseGeocodeLimit: Int? = null
//
//    override suspend fun getAutosuggest(request: HereAutosuggestRequest): HereAutosuggestResponse {
//        autosuggestCallCount++
//        return autosuggestHandler(request)
//    }
//
//    override suspend fun getAutocomplete(request: HereAutocompleteRequest): HereAutocompleteResponse {
//        autocompleteCallCount++
//        return autocompleteHandler(request)
//    }
//
//    override suspend fun reverseGeocode(
//        latitude: Double,
//        longitude: Double,
//        limit: Int?,
//    ): HereReverseGeocodeResponse {
//        reverseGeocodeCallCount++
//        lastReverseGeocodeLimit = limit
//        return reverseGeocodeHandler(latitude, longitude, limit)
//    }
//}
//
//private class FakeRoutePlannerApi : RoutePlannerApi {
//    override suspend fun getPublicTransportSuggestions(
//        location: LocationRequest,
//    ): List<PublicTransportSuggestionResponse> = emptyList()
//}
