package com.vie.mit.home

interface HomeNavigation {
    fun navigateToRideSearch()
    fun navigateToDetailRide(idRide: Int)
    fun navigateBack()

    fun navigateHomeToMainMap()
}