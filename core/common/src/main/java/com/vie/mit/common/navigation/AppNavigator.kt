package com.vie.mit.common.navigation

import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.channels.ReceiveChannel

interface AppNavigator {
    val navigationChannel: ReceiveChannel<NavigationEvent>
    fun navigateTo(route: Any, builder: NavOptionsBuilder.() -> Unit = {})
    fun navigateUp()
    fun popBackStack(route: Any? = null, inclusive: Boolean = false)
}

