package com.vie.mit.data.model.common.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PublicTransportType {
    @SerialName("Bus")
    BUS,

    @SerialName("Rail")
    RAIL,

    @SerialName("Subway")
    SUBWAY,

    @SerialName("Tram")
    TRAM,

    @SerialName("Ferry")
    FERRY,

    @SerialName("CableTram")
    CABLE_TRAM,

    @SerialName("AerialLift")
    AERIAL_LIFT,

    @SerialName("Monorail")
    MONORAIL,

    @SerialName("Trolleybus")
    TROLLEYBUS
}
