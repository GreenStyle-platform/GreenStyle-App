package com.vie.mit.green.navigation.auth

import com.vie.mit.auth.AuthNavigation
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.green.navigation.main.MainGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

class AuthNavigator @Inject constructor(
    private val appNavigator: AppNavigator
) : AuthNavigation {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun navigateToLogin() {
        scope.launch {
            appNavigator.navigateTo(Login) {
                popUpTo(Splash) { inclusive = true }
            }
        }
    }

    override fun navigateToHome() {
        scope.launch {
            appNavigator.navigateTo(MainGraph) {
                popUpTo(AuthGraph) { inclusive = true }
            }
        }
    }
}
