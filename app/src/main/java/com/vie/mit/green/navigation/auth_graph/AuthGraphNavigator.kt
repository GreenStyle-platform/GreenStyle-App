package com.vie.mit.green.navigation.auth_graph

import com.vie.mit.auth.AuthNavigation
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.green.navigation.main_graph.MainGraph
import javax.inject.Inject

class AuthGraphNavigator @Inject constructor(
    private val appNavigator: AppNavigator
) : AuthNavigation {
    override fun navigateToLoginFromSplash() {
        appNavigator.navigateTo(Login) {
            popUpTo(Splash) { inclusive = true }
        }
    }

    override fun navigateToHomeFromSplash() {
        appNavigator.navigateTo(MainGraph) {
            popUpTo(AuthGraph) { inclusive = true }
        }
    }

    override fun navigateToForgotPasswordFromLogin() {
        appNavigator.navigateTo(ForgotPassword)
    }

    override fun navigateToSignUpFromLogin() {
        appNavigator.navigateTo(SignUp)
    }

    override fun navigateBack() {
        appNavigator.navigateUp()
    }
}

