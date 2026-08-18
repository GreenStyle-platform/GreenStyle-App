package com.vie.mit.green.navigation

import androidx.navigation.NavOptionsBuilder
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.common.navigation.NavigationEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationManager @Inject constructor() : AppNavigator {
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    override suspend fun navigateTo(route: Any, builder: NavOptionsBuilder.() -> Unit) {
        _navigationEvents.emit(NavigationEvent.NavigateTo(route, builder))
    }

    override suspend fun navigateUp() {
        _navigationEvents.emit(NavigationEvent.NavigateUp)
    }

    override suspend fun popBackStack(route: Any?, inclusive: Boolean) {
        _navigationEvents.emit(NavigationEvent.PopBackStack(route, inclusive))
    }
}
