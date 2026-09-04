package com.vie.mit.green.navigation

import androidx.navigation.NavOptionsBuilder
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.common.navigation.NavigationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationManager @Inject constructor() : AppNavigator {
    private val _navigationChannel = Channel<NavigationEvent>(capacity = Channel.BUFFERED)
    override val navigationChannel: ReceiveChannel<NavigationEvent> = _navigationChannel

    override fun navigateTo(route: Any, builder: NavOptionsBuilder.() -> Unit) {
        _navigationChannel.trySend(NavigationEvent.NavigateTo(route, builder))
    }

    override fun navigateUp() {
        _navigationChannel.trySend(NavigationEvent.NavigateUp)
    }

    override fun popBackStack(route: Any?, inclusive: Boolean) {
        _navigationChannel.trySend(NavigationEvent.PopBackStack(route, inclusive))
    }
}

