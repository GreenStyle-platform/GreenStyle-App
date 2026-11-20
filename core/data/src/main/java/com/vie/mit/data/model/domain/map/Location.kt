package com.vie.mit.data.model.domain.map

data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val radius: Int = 100
)
