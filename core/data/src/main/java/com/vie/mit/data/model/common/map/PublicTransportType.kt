package com.vie.mit.data.model.common.map

import kotlinx.serialization.Serializable

@Serializable
enum class PublicTransportType {
    BUS, RAIL, SUBWAY, TRAM, FERRY, CABLE_TRAM, AERIAL_LIFT, MONORAIL, TROLLEYBUS
}
