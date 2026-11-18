package com.vie.mit.green.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.vie.mit.data.model.common.map.PublicTransportType
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.green.navigation.main_graph.HomeSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbyBikeStationsSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbyBusStopsSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbySubwayStationsSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbyTransitStopsSheetRoute
import com.vie.mit.green.navigation.main_graph.StopDetailSheetRoute
import com.vie.mit.green.ui.home.homesheet.HomeSheet
import com.vie.mit.green.ui.home.nearbybikestations.NearbyBikeStationsSheet
import com.vie.mit.green.ui.home.nearbybusstops.NearbyBusStopViewModel
import com.vie.mit.green.ui.home.nearbybusstops.NearbyBusStopsSheet
import com.vie.mit.green.ui.home.nearbysubwaystations.NearbySubwayStationsSheet
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyTransitSheet

/**
 * SheetNavHost: Nested NavHost nằm bên trong BottomSheet.
 * Mỗi destination là một nội dung Sheet riêng biệt, có thể navigate qua lại
 * với hiệu ứng chuyển cảnh dạng Bottom Sheet (truyền slide dọc từ dưới lên/xuống)
 * mà không ảnh hưởng đến Mapbox ở background.
 */
@Composable
fun SheetNavHost(
    sheetNavController: NavHostController,
    homeContainerViewModel: HomeContainerViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = sheetNavController,
        startDestination = HomeSheetRoute,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 6 },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 200))
        },
        exitTransition = {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight / 6 },
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 180))
        },
        popEnterTransition = {
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 6 },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 200))
        },
        popExitTransition = {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight / 6 },
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 180))
        },
    ) {
        composable<HomeSheetRoute> {
            val uiState by homeContainerViewModel.uiState.collectAsStateWithLifecycle()
            HomeSheet(
                activeTransitTypes = uiState.activeTransitTypes,
                onToggleTransitType = homeContainerViewModel::toggleTransitType,
                onTransitTypeClick = { typeId ->
                    homeContainerViewModel.selectTransitType(typeId)
                    when {
                        typeId.equals(PublicTransportType.BUS.name, ignoreCase = true) -> {
                            sheetNavController.navigate(NearbyBusStopsSheetRoute)
                        }
                        typeId.equals(PublicTransportType.SUBWAY.name, ignoreCase = true) -> {
                            sheetNavController.navigate(NearbySubwayStationsSheetRoute)
                        }
                        typeId.equals(TravelMode.BICYCLE_RENT.name, ignoreCase = true) -> {
                            sheetNavController.navigate(NearbyBikeStationsSheetRoute)
                        }
                        else -> {
                            sheetNavController.navigate(NearbyTransitStopsSheetRoute)
                        }
                    }
                },
            )
        }

        composable<NearbyBusStopsSheetRoute> {
            val homeUiState by homeContainerViewModel.uiState.collectAsStateWithLifecycle()
            val nearbyBusViewModel: NearbyBusStopViewModel = hiltViewModel()
            val busUiState by nearbyBusViewModel.uiState.collectAsStateWithLifecycle()

            DisposableEffect(Unit) {
                homeContainerViewModel.setNearbySheetVisible(true)
                onDispose {
                    homeContainerViewModel.setNearbySheetVisible(false)
                    homeContainerViewModel.clearTransitTypeSelection()
                }
            }

            LaunchedEffect(homeUiState.nearbyStops) {
                // homeUiState.nearbyStops đã được ViewModel lọc và sắp xếp theo khoảng cách cho riêng Bus
                nearbyBusViewModel.loadBusStops(homeUiState.nearbyStops)
            }

            NearbyBusStopsSheet(
                uiState = busUiState,
                selectedRadiusKm = homeUiState.selectedRadiusKm,
                onRadiusSelected = homeContainerViewModel::setSelectedRadiusKm,
                onBack = { sheetNavController.popBackStack() },
                onRefresh = nearbyBusViewModel::refresh,
            )
        }

        composable<NearbySubwayStationsSheetRoute> {
            val homeUiState by homeContainerViewModel.uiState.collectAsStateWithLifecycle()

            DisposableEffect(Unit) {
                homeContainerViewModel.setNearbySheetVisible(true)
                onDispose {
                    homeContainerViewModel.setNearbySheetVisible(false)
                    homeContainerViewModel.clearTransitTypeSelection()
                }
            }

            NearbySubwayStationsSheet(
                nearbyStops = homeUiState.nearbyStops,
                selectedRadiusKm = homeUiState.selectedRadiusKm,
                onRadiusSelected = homeContainerViewModel::setSelectedRadiusKm,
                onBack = { sheetNavController.popBackStack() },
                onStopClick = { stop ->
                    sheetNavController.navigate(
                        StopDetailSheetRoute(
                            stopId = stop.stopId,
                            typeId = stop.typeId,
                            name = stop.name,
                        )
                    )
                },
            )
        }

        composable<NearbyBikeStationsSheetRoute> {
            val homeUiState by homeContainerViewModel.uiState.collectAsStateWithLifecycle()

            DisposableEffect(Unit) {
                homeContainerViewModel.setNearbySheetVisible(true)
                onDispose {
                    homeContainerViewModel.setNearbySheetVisible(false)
                    homeContainerViewModel.clearTransitTypeSelection()
                }
            }

            NearbyBikeStationsSheet(
                nearbyStops = homeUiState.nearbyStops,
                selectedRadiusKm = homeUiState.selectedRadiusKm,
                onRadiusSelected = homeContainerViewModel::setSelectedRadiusKm,
                onBack = { sheetNavController.popBackStack() },
                onStopClick = { stop ->
                    sheetNavController.navigate(
                        StopDetailSheetRoute(
                            stopId = stop.stopId,
                            typeId = stop.typeId,
                            name = stop.name,
                        )
                    )
                },
            )
        }

        composable<NearbyTransitStopsSheetRoute> {
            val uiState by homeContainerViewModel.uiState.collectAsStateWithLifecycle()

            // Đồng bộ trạng thái hiển thị của NearbyTransitSheet với ViewModel
            DisposableEffect(Unit) {
                homeContainerViewModel.setNearbySheetVisible(true)
                onDispose {
                    homeContainerViewModel.setNearbySheetVisible(false)
                    homeContainerViewModel.clearTransitTypeSelection()
                }
            }

            NearbyTransitSheet(
                nearbyStops = uiState.nearbyStops,
                selectedRadiusKm = uiState.selectedRadiusKm,
                onRadiusSelected = homeContainerViewModel::setSelectedRadiusKm,
                onBack = { sheetNavController.popBackStack() },
                onStopClick = { stop ->
                    sheetNavController.navigate(
                        StopDetailSheetRoute(
                            stopId = stop.stopId,
                            typeId = stop.typeId,
                            name = stop.name,
                        )
                    )
                },
            )
        }

        composable<StopDetailSheetRoute> { entry ->
            val route = entry.toRoute<StopDetailSheetRoute>()
            StopDetailSheetContent(
                stopId = route.stopId,
                typeId = route.typeId,
                name = route.name,
                onBack = { sheetNavController.popBackStack() },
            )
        }
    }
}
