package com.vie.mit.common.navigation

import androidx.navigation.NavOptionsBuilder

sealed class NavigationEvent {
    data class NavigateTo(
        val route: Any,
        val builder: NavOptionsBuilder.() -> Unit = {}
    ) : NavigationEvent()

    data object NavigateUp : NavigationEvent()

    data class PopBackStack(
        val route: Any? = null,
        val inclusive: Boolean = false
    ) : NavigationEvent()
}
