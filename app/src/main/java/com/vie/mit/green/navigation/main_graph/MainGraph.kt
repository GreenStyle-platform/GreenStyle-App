package com.vie.mit.green.navigation.main_graph

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.vie.mit.green.ui.home.HomeContainerScreen
import com.vie.mit.green.ui.home.HomeContainerViewModel

fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<MainGraph>(
        startDestination = MapContainerRoute
    ) {
        composable<MapContainerRoute> {
            HomeContainerScreen(
                homeContainerViewModel = hiltViewModel<HomeContainerViewModel>(),
            )
        }
    }
}
