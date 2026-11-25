package com.vie.mit.green.ui.routeplanner.container

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vie.mit.green.navigation.main_graph.RouteLocationPickerSheetRoute
import com.vie.mit.green.navigation.main_graph.RouteOptionsRouteSheet
import com.vie.mit.green.ui.routeplanner.routelocationpicker.RouteLocationPickerSheet
import com.vie.mit.green.ui.routeplanner.routeoptions.RouteOptionsSheet

/**
 * RoutePlannerNavHost: Nested NavHost điều hướng giữa 2 sheet:
 * 1. [RouteLocationPickerSheetRoute] ➔ [RouteLocationPickerSheet] (chọn điểm xuất phát và điểm đích).
 * 2. [RouteOptionsRouteSheet] ➔ [RouteOptionsSheet] (hiển thị danh sách các phương án di chuyển).
 */
@Composable
fun RoutePlannerNavHost(
    routePlannerNavController: NavHostController,
    routePlannerContainerViewModel: RoutePlannerContainerViewModel,
    onSearchFieldClicked: () -> Unit = {},
    onPickOnMapClicked: () -> Unit = {},
    onNavigateToRouteOptions: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = routePlannerNavController,
        startDestination = RouteLocationPickerSheetRoute,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 6 },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(durationMillis = 200))
        },
        exitTransition = {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight / 6 },
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(durationMillis = 180))
        },
        popEnterTransition = {
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 6 },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(durationMillis = 200))
        },
        popExitTransition = {
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight / 6 },
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(durationMillis = 180))
        },
    ) {
        composable<RouteLocationPickerSheetRoute> {
            RouteLocationPickerSheet(
                containerViewModel = routePlannerContainerViewModel,
                onSearchFieldClicked = onSearchFieldClicked,
                onPickOnMapClicked = onPickOnMapClicked,
                onNavigateToRouteOptions = {
                    onNavigateToRouteOptions()
                    routePlannerNavController.navigate(RouteOptionsRouteSheet)
                },
            )
        }

        composable<RouteOptionsRouteSheet> {
            RouteOptionsSheet(
                containerViewModel = routePlannerContainerViewModel,
                onBackToPicker = { fieldToFocus ->
                    if (fieldToFocus != null) {
                        routePlannerContainerViewModel.setFocusedField(fieldToFocus)
                    }
                    routePlannerNavController.popBackStack()
                },
            )
        }
    }
}
