package com.vie.mit.green.homecontainer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vie.mit.chat.ChatScreen
import com.vie.mit.chat.ChatViewModel
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.green.navigation.main_graph.ChatTab
import com.vie.mit.green.navigation.main_graph.HomeTab
import com.vie.mit.green.navigation.main_graph.MapTab
import com.vie.mit.green.navigation.main_graph.RideTab
import com.vie.mit.home.home.HomeScreen
import com.vie.mit.home.home.HomeViewModel
import com.vie.mit.map.MapScreen
import com.vie.mit.map.MapViewModel
import com.vie.mit.ride.RideScreen
import com.vie.mit.ride.RideViewModel

@Composable
fun HomeContainerScreen(
    viewModel: HomeContainerViewModel = hiltViewModel()
) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val items = listOf(
        BottomNavItem.Home, BottomNavItem.Map, BottomNavItem.Ride, BottomNavItem.Chat
    )

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 16.dp)
                    .background(
                        color = AppTheme.colors.background
                    )
                    .padding(start = 2.dp, bottom = 16.dp, top = 8.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any {
                        it.hasRoute(item.route::class)
                    } == true
                    BottomNavigationItem(
                        modifier = Modifier.weight(1f),
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            innerNavController.navigate(item.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        })
                }
            }
        }) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = HomeTab,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<HomeTab> {
                HomeScreen(hiltViewModel<HomeViewModel>())
            }
            composable<RideTab> {
                RideScreen(hiltViewModel<RideViewModel>())
            }
            composable<MapTab> {
                MapScreen(hiltViewModel<MapViewModel>())
            }
            composable<ChatTab> {
                ChatScreen(hiltViewModel<ChatViewModel>())
            }
        }
    }
}
