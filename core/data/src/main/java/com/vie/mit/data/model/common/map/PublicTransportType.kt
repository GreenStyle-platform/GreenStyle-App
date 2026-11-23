package com.vie.mit.data.model.common.map

import com.vie.mit.mapbox.model.TransitTypeConfig
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

/**
 * Converts a [PublicTransportType] enum value to its corresponding visual [TransitTypeConfig].
 */
fun PublicTransportType.toTransitTypeConfig(iconSize: Double = 1.2): TransitTypeConfig {
    return when (this) {
        PublicTransportType.SUBWAY, PublicTransportType.MONORAIL -> TransitTypeConfig(
            typeId = PublicTransportType.SUBWAY.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_metro,
            clusterColor = 0xFFE53935.toInt(),
            iconSize = iconSize,
        )

        PublicTransportType.RAIL, PublicTransportType.TRAM, PublicTransportType.CABLE_TRAM, PublicTransportType.AERIAL_LIFT -> TransitTypeConfig(
            typeId = PublicTransportType.RAIL.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_train,
            clusterColor = 0xFF8E24AA.toInt(),
            iconSize = iconSize,
        )

        PublicTransportType.FERRY -> TransitTypeConfig(
            typeId = PublicTransportType.FERRY.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bus,
            clusterColor = 0xFF00ACC1.toInt(),
            iconSize = iconSize,
        )

        else -> TransitTypeConfig(
            typeId = PublicTransportType.BUS.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bus,
            clusterColor = 0xFF1E88E5.toInt(),
            iconSize = iconSize,
        )
    }
}

