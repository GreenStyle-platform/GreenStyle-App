package com.vie.mit.green.navigation.main_graph

import com.vie.mit.data.model.common.map.PublicTransportType
import kotlinx.serialization.Serializable

@Serializable
object MainGraph

@Serializable
object MapContainerRoute

// Sheet routes (dùng trong Nested NavHost bên trong BottomSheet)
@Serializable
object HomeSheetRoute

@Serializable
data object NearbyTransitStopsSheetRoute

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
data class StopDetailSheetRoute(
    val stopId: String,
    val typeId: String = "",
    val name: String = "",
)

@Serializable
object SettingRoute

@Serializable
object BusRouteSearchRoute

@Serializable
object JourneyDetailRoute

@Serializable
object JourneyNavigationRoute

@Serializable
object TransitRoutePathRoute

@Serializable
data class TransitStopDetailRoute(
    val stopId: String,
    val typeId: PublicTransportType = PublicTransportType.BUS,
)
