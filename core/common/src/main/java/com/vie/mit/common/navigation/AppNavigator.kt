package com.vie.mit.common.navigation

import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.flow.SharedFlow

interface AppNavigator {
    val navigationEvents: SharedFlow<NavigationEvent>
    suspend fun navigateTo(route: Any, builder: NavOptionsBuilder.() -> Unit = {})
    suspend fun navigateUp()
    suspend fun popBackStack(route: Any? = null, inclusive: Boolean = false)
}
