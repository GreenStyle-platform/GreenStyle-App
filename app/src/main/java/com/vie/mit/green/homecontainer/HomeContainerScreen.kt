package com.vie.mit.green.homecontainer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.TimeToLeave
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.vie.mit.green.navigation.main_graph.ChatTab
import com.vie.mit.green.navigation.main_graph.HomeTab
import com.vie.mit.green.navigation.main_graph.MapTab
import com.vie.mit.green.navigation.main_graph.RideTab
import com.vie.mit.home.HomeScreen
import com.vie.mit.home.HomeViewModel
import com.vie.mit.map.MapScreen
import com.vie.mit.map.MapViewModel
import com.vie.mit.ride.RideScreen
import com.vie.mit.ride.RideViewModel

sealed class BottomNavItem(val route: Any, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(HomeTab, Icons.Default.Home, "Home")
    object Ride : BottomNavItem(RideTab, Icons.Default.TimeToLeave, "Ride")
    object Map : BottomNavItem(MapTab, Icons.Default.Map, "Map")
    object Chat : BottomNavItem(ChatTab, Icons.Default.Chat, "Chat")
}

@Composable
fun HomeContainerScreen(
    viewModel: HomeContainerViewModel = hiltViewModel()
) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Ride,
        BottomNavItem.Map,
        BottomNavItem.Chat
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { 
                        it.hasRoute(item.route::class) 
                    } == true
                    
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = {
                            innerNavController.navigate(item.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
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
