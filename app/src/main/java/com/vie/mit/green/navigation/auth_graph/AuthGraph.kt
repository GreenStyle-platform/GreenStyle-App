package com.vie.mit.green.navigation.auth_graph

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.vie.mit.auth.login.LoginScreen
import com.vie.mit.auth.login.LoginViewModel
import com.vie.mit.auth.splash.SplashScreen
import com.vie.mit.auth.splash.SplashViewModel

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation<AuthGraph>(
        startDestination = Splash
    ) {
        composable<Splash> {
            SplashScreen(viewModel = hiltViewModel<SplashViewModel>())
        }
        composable<Login> {
            LoginScreen(
                viewModel = hiltViewModel<LoginViewModel>()
            )
        }
    }
}
