package com.vie.mit.green.navigation.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation

fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation<MainGraph>(
        startDestination = Home
    ) {
        composable<Home> {
            HomeScreen()
        }
    }
}
