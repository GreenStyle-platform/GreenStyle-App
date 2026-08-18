package com.vie.mit.green.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.vie.mit.green.navigation.auth.AuthGraph
import com.vie.mit.green.navigation.auth.authGraph
import com.vie.mit.green.navigation.main.mainGraph

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController, startDestination = AuthGraph
    ) {
        authGraph(navController)
        mainGraph(navController)
    }
}
