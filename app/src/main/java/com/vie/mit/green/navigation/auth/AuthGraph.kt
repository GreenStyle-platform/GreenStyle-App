package com.vie.mit.green.navigation.auth

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.vie.mit.auth.login.LoginScreen
import com.vie.mit.auth.splash.SplashScreen

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation<AuthGraph>(
        startDestination = Splash
    ) {
        composable<Splash> {
            SplashScreen()
        }
        composable<Login> {
            LoginScreen(
            )
        }
    }
}
