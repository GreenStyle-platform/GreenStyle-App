package com.vie.mit.green.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.vie.mit.green.navigation.auth_graph.authGraph
import com.vie.mit.green.navigation.main_graph.MainGraph
import com.vie.mit.green.navigation.main_graph.mainGraph

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController, startDestination = MainGraph
    ) {
        authGraph(navController)
        mainGraph(navController)
    }
}
