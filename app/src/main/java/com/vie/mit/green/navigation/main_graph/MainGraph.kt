package com.vie.mit.green.navigation.main_graph

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.vie.mit.common.navigation.Arguments
import com.vie.mit.green.homecontainer.HomeContainerScreen
import com.vie.mit.ride.DetailRideScreen
import com.vie.mit.ride.DetailRideViewModel

fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<MainGraph>(
        startDestination = HomeContainer
    ) {
        composable<HomeContainer> {
            HomeContainerScreen()
        }

        composable<DetailRide> { backStackEntry ->
            val route: DetailRide = backStackEntry.toRoute<DetailRide>()
            backStackEntry.savedStateHandle[Arguments.ID_RIDE] = route.idRide
            DetailRideScreen(
                idRide = route.idRide,
                viewModel = hiltViewModel<DetailRideViewModel>(),
            )
        }
    }
}

