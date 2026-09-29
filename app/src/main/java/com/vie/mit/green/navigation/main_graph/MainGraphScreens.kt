package com.vie.mit.green.navigation.main_graph

import kotlinx.serialization.Serializable

@Serializable
object MainGraph

@Serializable
object HomeContainer

@Serializable
object HomeTab

@Serializable
object RideTab

@Serializable
object MapTab

@Serializable
object ChatTab

@Serializable
object Home

@Serializable
object RideSearchResult

@Serializable
data class DetailRide(val idRide: Int)

@Serializable
object MainMap