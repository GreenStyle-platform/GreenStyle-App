package com.vie.mit.data.model.common.map

import com.vie.mit.mapbox.model.TransitTypeConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TransitMode {
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
    TROLLEYBUS,

    @SerialName("Bike")
    BIKE,

    @SerialName("Walk")
    WALK;

    val isTransit: Boolean
        get() = this != WALK && this != BIKE

    companion object {
        /**
         * Parses an OpenTripPlanner 2 leg mode string (e.g. "WALK", "BUS", "SUBWAY", "BICYCLE",
         * "RAIL", "TRAM", "FERRY", "CABLE_CAR", "GONDOLA", "MONORAIL", "TROLLEYBUS", "COACH")
         * into a [TransitMode].
         */
        fun fromOtpMode(rawMode: String?): TransitMode = when (rawMode?.trim()?.uppercase()) {
            "WALK" -> WALK
            "BICYCLE", "BIKE", "BICYCLE_RENT", "SCOOTER", "SCOOTER_RENTAL" -> BIKE
            "SUBWAY", "METRO" -> SUBWAY
            "RAIL", "TRAIN", "FUNICULAR" -> RAIL
            "TRAM" -> TRAM
            "FERRY" -> FERRY
            "CABLE_CAR", "CABLE_TRAM", "CABLETRAM" -> CABLE_TRAM
            "GONDOLA", "AERIAL_LIFT", "AERIALLIFT" -> AERIAL_LIFT
            "MONORAIL" -> MONORAIL
            "TROLLEYBUS" -> TROLLEYBUS
            "BUS", "COACH", "FLEX" -> BUS
            else -> WALK
        }
    }
}

/**
 * Converts a [TransitMode] enum value to its corresponding visual [TransitTypeConfig].
 */
fun TransitMode.toTransitTypeConfig(iconSize: Double = 1.2): TransitTypeConfig {
    return when (this) {
        TransitMode.SUBWAY, TransitMode.MONORAIL -> TransitTypeConfig(
            typeId = TransitMode.SUBWAY.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_metro,
            clusterColor = 0xFFE53935.toInt(),
            iconSize = iconSize,
        )

        TransitMode.RAIL, TransitMode.TRAM, TransitMode.CABLE_TRAM, TransitMode.AERIAL_LIFT -> TransitTypeConfig(
            typeId = TransitMode.RAIL.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_train,
            clusterColor = 0xFF8E24AA.toInt(),
            iconSize = iconSize,
        )

        TransitMode.FERRY -> TransitTypeConfig(
            typeId = TransitMode.FERRY.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bus,
            clusterColor = 0xFF00ACC1.toInt(),
            iconSize = iconSize,
        )

        else -> TransitTypeConfig(
            typeId = TransitMode.BUS.name,
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bus,
            clusterColor = 0xFF1E88E5.toInt(),
            iconSize = iconSize,
        )
    }
}

