package com.vie.mit.green.navigation.main

import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.home.HomeNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainNavigator @Inject constructor(
    private val appNavigator: AppNavigator
) : HomeNavigation {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun navigateToRideSearch() {
        scope.launch {
            appNavigator.navigateTo(RideSearchResult)
        }
    }
}
