package com.vie.mit.green.navigation.main_graph

import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.home.HomeNavigation
import javax.inject.Inject

class MainGraphNavigator @Inject constructor(
    private val appNavigator: AppNavigator
) : HomeNavigation {
    override fun navigateToRideSearch() {
        appNavigator.navigateTo(RideSearchResult)
    }

    override fun navigateToDetailRide(idRide: Int) {
        appNavigator.navigateTo(DetailRide(idRide = idRide))
    }

    override fun navigateBack() {
        appNavigator.navigateUp()
    }

    override fun navigateHomeToMainMap() {
        appNavigator.navigateTo(MainMap)
    }
}

