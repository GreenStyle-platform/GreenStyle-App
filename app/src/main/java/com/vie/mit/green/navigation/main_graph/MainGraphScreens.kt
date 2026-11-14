package com.vie.mit.green.navigation.main_graph

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

@Serializable
data class StopDetailSheetRoute(
    val stopId: String,
    val typeId: String = "",
    val name: String = "",
)
