package com.vie.mit.green.navigation.auth_graph

import com.vie.mit.auth.AuthNavigation
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.green.navigation.main_graph.MainGraph
import javax.inject.Inject

class AuthGraphNavigator @Inject constructor(
    private val appNavigator: AppNavigator
) : AuthNavigation {
    override fun navigateToLogin() {
        appNavigator.navigateTo(Login) {
            popUpTo(Splash) { inclusive = true }
        }
    }

    override fun navigateToHome() {
        appNavigator.navigateTo(MainGraph) {
            popUpTo(AuthGraph) { inclusive = true }
        }
    }
}

