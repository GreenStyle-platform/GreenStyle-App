package com.vie.mit.data.model.common.map

import kotlinx.serialization.Serializable

@Serializable
enum class TravelMode {
    WALK, BICYCLE, BICYCLE_RENT, SCOOTER_RENTAL, CAR
}