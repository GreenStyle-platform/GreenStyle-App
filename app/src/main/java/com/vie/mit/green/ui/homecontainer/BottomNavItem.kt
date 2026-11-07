package com.vie.mit.green.ui.homecontainer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.vie.mit.green.R
import com.vie.mit.green.navigation.main_graph.ChatTab
import com.vie.mit.green.navigation.main_graph.HomeTab
import com.vie.mit.green.navigation.main_graph.RideTab

sealed class BottomNavItem(
    val route: Any, @param:DrawableRes val icon: Int, @param:StringRes val label: Int
) {
    object Home : BottomNavItem(HomeTab, R.drawable.ic_home, R.string.home)
    object Ride : BottomNavItem(RideTab, R.drawable.ic_ride, R.string.ride)
    object Chat : BottomNavItem(ChatTab, R.drawable.ic_chat, R.string.chat)
}