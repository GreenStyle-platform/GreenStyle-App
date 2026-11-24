package com.vie.mit.green.navigation.main_graph

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.vie.mit.green.ui.busroutesearch.BusRouteSearchScreen
import com.vie.mit.green.ui.busroutesearch.BusRouteSearchViewModel
import com.vie.mit.green.ui.home.container.HomeContainerScreen
import com.vie.mit.green.ui.home.container.HomeContainerViewModel
import com.vie.mit.green.ui.journeydetail.JourneyDetailScreen
import com.vie.mit.green.ui.journeydetail.JourneyDetailViewModel
import com.vie.mit.green.ui.journeynavigation.JourneyNavigationScreen
import com.vie.mit.green.ui.journeynavigation.JourneyNavigationViewModel
import com.vie.mit.green.ui.setting.SettingScreen
import com.vie.mit.green.ui.setting.SettingViewModel
import com.vie.mit.green.ui.transitroutedetail.TransitRouteDetailViewModel
import com.vie.mit.green.ui.transitroutedetail.TransitRoutePathScreen
import com.vie.mit.green.ui.transitstopdetail.TransitStopDetailScreen
import com.vie.mit.green.ui.transitstopdetail.TransitStopDetailViewModel

fun NavGraphBuilder.mainGraph() {
    navigation<MainGraph>(
        startDestination = MapContainerRoute
    ) {
        composable<MapContainerRoute> {
            HomeContainerScreen(
                homeContainerViewModel = hiltViewModel<HomeContainerViewModel>(),
            )
        }

        composable<SettingRoute> {
            SettingScreen(
                viewModel = hiltViewModel<SettingViewModel>(),
            )
        }

        composable<BusRouteSearchRoute> {
            BusRouteSearchScreen(
                viewModel = hiltViewModel<BusRouteSearchViewModel>(),
            )
        }

        composable<JourneyDetailRoute> {
            JourneyDetailScreen(
                viewModel = hiltViewModel<JourneyDetailViewModel>(),
            )
        }

        composable<JourneyNavigationRoute> {
            JourneyNavigationScreen(
                viewModel = hiltViewModel<JourneyNavigationViewModel>(),
            )
        }

        composable<TransitRouteDetailRoute>(
            enterTransition = { fadeIn(tween(150)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(150)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            TransitRoutePathScreen(
                viewModel = hiltViewModel<TransitRouteDetailViewModel>(),
            )
        }

        composable<TransitRoutePathRoute>(
            enterTransition = { fadeIn(tween(150)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(150)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            TransitRoutePathScreen(
                viewModel = hiltViewModel<TransitRouteDetailViewModel>(),
            )
        }

        composable<TransitStopDetailRoute>(
            enterTransition = { fadeIn(tween(150)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(150)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            TransitStopDetailScreen(
                viewModel = hiltViewModel<TransitStopDetailViewModel>(),
            )
        }
    }
}
