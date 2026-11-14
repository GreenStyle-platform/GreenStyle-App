package com.vie.mit.green.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.vie.mit.green.navigation.main_graph.HomeSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbyStopsSheetRoute
import com.vie.mit.green.navigation.main_graph.StopDetailSheetRoute
import com.vie.mit.green.ui.home.homesheet.HomeSheet
import com.vie.mit.green.ui.home.homesheet.HomeSheetUiState
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyTransitSheet

/**
 * SheetNavHost: Nested NavHost nằm bên trong BottomSheet.
 * Mỗi destination là một nội dung Sheet riêng biệt, có thể navigate qua lại
 * mà không ảnh hưởng đến Mapbox ở background.
 */
@Composable
fun SheetNavHost(
    sheetNavController: NavHostController,
    homeContainerViewModel: HomeContainerViewModel,
    uiState: HomeSheetUiState,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = sheetNavController,
        startDestination = HomeSheetRoute,
        modifier = modifier.fillMaxWidth(),
    ) {
        composable<HomeSheetRoute> {
            HomeSheet(
                activeTransitTypes = uiState.activeTransitTypes,
                onToggleTransitType = homeContainerViewModel::toggleTransitType,
            )
        }

        composable<NearbyStopsSheetRoute> {
            NearbyTransitSheet(
                nearbyStops = uiState.nearbyStops,
                selectedRadiusKm = uiState.selectedRadiusKm,
                onRadiusSelected = homeContainerViewModel::setSelectedRadiusKm,
                onBack = { sheetNavController.popBackStack() },
                onStopClick = { stop ->
                    sheetNavController.navigate(
                        StopDetailSheetRoute(
                            stopId = stop.stopId,
                            typeId = stop.typeId,
                            name = stop.name,
                        )
                    )
                },
            )
        }

        composable<StopDetailSheetRoute> { entry ->
            val route = entry.toRoute<StopDetailSheetRoute>()
            StopDetailSheetContent(
                stopId = route.stopId,
                typeId = route.typeId,
                name = route.name,
                onBack = { sheetNavController.popBackStack() },
            )
        }
    }
}
