package com.vie.mit.green.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.green.navigation.main_graph.HomeSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbyBikeStationsSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbyBusStopsSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbySubwayStationsSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbyTransitStopsSheetRoute
import com.vie.mit.green.ui.home.container.HomeContainerViewModel
import com.vie.mit.green.ui.home.homesheet.HomeSheet
import com.vie.mit.green.ui.home.nearbybikestations.NearbyBikeStationsSheet
import com.vie.mit.green.ui.home.nearbybusstops.NearbyBusStopViewModel
import com.vie.mit.green.ui.home.nearbybusstops.NearbyBusStopsSheet
import com.vie.mit.green.ui.home.nearbysubwaystations.NearbySubwayStationViewModel
import com.vie.mit.green.ui.home.nearbysubwaystations.NearbySubwayStationsSheet
import com.vie.mit.green.ui.home.nearbysubwaystations.SubwaySheetTab
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyTransitSheet
import timber.log.Timber

/**
 * SheetNavHost: Nested NavHost nằm bên trong BottomSheet.
 * Mỗi destination là một nội dung Sheet riêng biệt, có thể navigate qua lại
 * với hiệu ứng chuyển cảnh dạng Bottom Sheet (truyền slide dọc từ dưới lên/xuống)
 * mà không ảnh hưởng đến Mapbox ở background.
 */
@Composable
fun SheetNavHost(
    homeNavController: NavHostController,
    homeContainerViewModel: HomeContainerViewModel,
    modifier: Modifier = Modifier,
    onSubwayTabChanged: (SubwaySheetTab) -> Unit = {},
) {
    NavHost(
        navController = homeNavController,
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
                onSearchClick = homeContainerViewModel::navigateToRouteLocationPicker,
                onTransitTypeClick = { typeId ->
                    when {
                        typeId.equals(TransitMode.BUS.name, ignoreCase = true) -> {
                            homeNavController.navigate(NearbyBusStopsSheetRoute)
                        }

                        typeId.equals(TransitMode.SUBWAY.name, ignoreCase = true) -> {
                            homeNavController.navigate(NearbySubwayStationsSheetRoute)
                        }

                        typeId.equals(TravelMode.BICYCLE_RENT.name, ignoreCase = true) -> {
                            homeNavController.navigate(NearbyBikeStationsSheetRoute)
                        }

                        else -> {
                            homeNavController.navigate(NearbyTransitStopsSheetRoute(transitTypeId = typeId))
                        }
                    }
                },
            )
        }

        composable<NearbyBusStopsSheetRoute> {
            val homeUiState by homeContainerViewModel.uiState.collectAsStateWithLifecycle()
            val nearbyBusViewModel: NearbyBusStopViewModel = hiltViewModel()
            val busUiState by nearbyBusViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(homeUiState.nearbyStops, homeUiState.selectedTransitTypeId) {
                if (homeUiState.selectedTransitTypeId == TransitMode.BUS.name) {
                    Timber.d("PhucTH: loadBusStop: called")
                    nearbyBusViewModel.loadBusStops(homeUiState.nearbyStops)
                }
            }

            NearbyBusStopsSheet(
                uiState = busUiState,
                isLoadingStops = homeUiState.isLoadingTransit,
                selectedRadiusKm = homeUiState.selectedRadiusKm,
                onBack = {
                    homeNavController.popBackStack()
                },
                onRefresh = nearbyBusViewModel::refresh,
                onFindBusRouteClick = homeContainerViewModel::navigateToBusRouteSearch,
                onNavigateToTransitStopDetail = nearbyBusViewModel::navigateToTransitStopDetail
            )
        }

        composable<NearbySubwayStationsSheetRoute> {
            val homeUiState by homeContainerViewModel.uiState.collectAsStateWithLifecycle()
            val subwayViewModel: NearbySubwayStationViewModel = hiltViewModel()
            val subwayUiState by subwayViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(subwayUiState.selectedTab) {
                onSubwayTabChanged(subwayUiState.selectedTab)
            }

            NearbySubwayStationsSheet(
                nearbyStops = homeUiState.nearbyStops,
                isLoadingStops = homeUiState.isLoadingTransit,
                uiState = subwayUiState,
                selectedRadiusKm = homeUiState.selectedRadiusKm,
                onBack = {
                    homeNavController.popBackStack()
                },
                onTabSelected = subwayViewModel::onTabChanged,
                onRetryLoadRoutes = subwayViewModel::loadSubwayRoutes,
                onRouteClick = subwayViewModel::navigateToRouteDetail,
                onStopClick = subwayViewModel::navigateToTransitStopDetail,
            )
        }

        composable<NearbyBikeStationsSheetRoute> {
            val homeUiState by homeContainerViewModel.uiState.collectAsStateWithLifecycle()

            NearbyBikeStationsSheet(
                nearbyStops = homeUiState.nearbyStops,
                isLoadingStops = homeUiState.isLoadingTransit,
                selectedRadiusKm = homeUiState.selectedRadiusKm,
                onBack = {
                    homeNavController.popBackStack()
                },
                onStopClick = homeContainerViewModel::focusOnNearbyStop,
            )
        }

        composable<NearbyTransitStopsSheetRoute> {
            val uiState by homeContainerViewModel.uiState.collectAsStateWithLifecycle()

            NearbyTransitSheet(
                nearbyStops = uiState.nearbyStops,
                isLoadingStops = uiState.isLoadingTransit,
                selectedRadiusKm = uiState.selectedRadiusKm,
                onBack = {
                    homeNavController.popBackStack()
                },
                onStopClick = homeContainerViewModel::onNearbyStopSelected,
            )
        }

    }
}
