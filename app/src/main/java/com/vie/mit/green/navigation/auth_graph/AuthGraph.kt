package com.vie.mit.green.navigation.auth_graph

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.vie.mit.auth.forgotpassword.ForgotPasswordScreen
import com.vie.mit.auth.forgotpassword.ForgotPasswordViewModel
import com.vie.mit.auth.login.LoginScreen
import com.vie.mit.auth.login.LoginViewModel
import com.vie.mit.auth.signup.SignUpScreen
import com.vie.mit.auth.signup.SignUpViewModel
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

        composable<ForgotPassword> {
            ForgotPasswordScreen(
                viewModel = hiltViewModel<ForgotPasswordViewModel>()
            )
        }

        composable<SignUp> {
            SignUpScreen(viewModel = hiltViewModel<SignUpViewModel>())
        }
    }
}
