package com.vie.mit.green.navigation.main_graph

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.vie.mit.green.ui.home.HomeScreen
import com.vie.mit.green.ui.home.HomeViewModel

fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<MainGraph>(
        startDestination = Home
    ) {
        composable<Home> {
            HomeScreen(viewModel = hiltViewModel<HomeViewModel>())
        }
    }
}

