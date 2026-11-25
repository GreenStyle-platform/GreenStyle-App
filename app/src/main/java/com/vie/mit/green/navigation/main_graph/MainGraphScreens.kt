package com.vie.mit.green.navigation.main_graph

import com.vie.mit.data.model.common.map.TransitMode
import kotlinx.serialization.Serializable

@Serializable
object MainGraph

@Serializable
object MapContainerRoute

// Sheet routes (dùng trong Nested NavHost bên trong BottomSheet)
@Serializable
object HomeSheetRoute

@Serializable
data class NearbyTransitStopsSheetRoute(
    val transitTypeId: String? = null,
)

/**
 * Route to NearbyBusStopsSheet — dedicated screen for nearby bus stops with arrival info.
 */
@Serializable
data object NearbyBusStopsSheetRoute

/**
 * Route to NearbySubwayStationsSheet — dedicated screen for nearby metro / subway stations.
 */
@Serializable
data object NearbySubwayStationsSheetRoute

/**
 * Route to NearbyBikeStationsSheet — dedicated screen for nearby bicycle rental stations.
 */
@Serializable
data object NearbyBikeStationsSheetRoute

@Serializable
object SettingRoute

@Serializable
object BusRouteSearchRoute

@Serializable
object RoutePlannerContainerRoute

// RoutePlanner nested sheet routes
@Serializable
object RouteLocationPickerSheetRoute

@Serializable
object RouteOptionsRouteSheet

@Serializable
object JourneyDetailRoute

@Serializable
object JourneyNavigationRoute

@Serializable
data class TransitRouteDetailRoute(
    val routeId: String = "",
    val transitMode: TransitMode = TransitMode.BUS,
)

@Serializable
object TransitRoutePathRoute

@Serializable
data class TransitStopDetailRoute(
    val stopId: String,
    val transitMode: TransitMode = TransitMode.BUS,
)
