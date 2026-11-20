package com.vie.mit.data.model.mapper

import com.vie.mit.data.model.domain.map.BusRoute
import com.vie.mit.data.model.domain.map.Location
import com.vie.mit.data.model.domain.map.PublicTransportSuggestion
import com.vie.mit.data.model.domain.map.StopArrival
import com.vie.mit.data.model.domain.map.TransitStopDetail
import com.vie.mit.data.model.domain.map.TransitStopMarker
import com.vie.mit.data.model.domain.map.VehicleArrival
import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.response.map.BusRouteResponse
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.RouteArrivalResponse
import com.vie.mit.data.model.remote.response.map.StopTimeResponse
import com.vie.mit.data.model.remote.response.map.TransitStopDetailResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import java.time.LocalTime
import java.util.Locale

fun Location.toLocationRequest(): LocationRequest = LocationRequest(
    latitude = latitude,
    longitude = longitude,
    altitude = altitude,
    radius = radius
)

fun PublicTransportSuggestionResponse.toDomain(): PublicTransportSuggestion = PublicTransportSuggestion(
    name = name,
    type = type,
    distance = distance,
    availableIn = availableIn
)

fun TransitStopMarkerResponse.toDomain(): TransitStopMarker = TransitStopMarker(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude
)

fun BusRouteResponse.toDomain(): BusRoute = BusRoute(
    routeId = routeId,
    routeShortName = routeShortName.orEmpty(),
    routeLongName = routeLongName.orEmpty(),
    routeDesc = routeDesc.orEmpty(),
    routeColor = routeColor,
    routeTextColor = routeTextColor,
)

fun Map<String, List<RouteArrivalResponse>>.toDomain(): List<StopArrival> {
    val now = LocalTime.now()
    val nowSeconds = now.toSecondOfDay()

    return flatMap { (stopId, routeResponses) ->
        routeResponses.map { route ->
            val allArrivals = route.stopTimeResponses.mapNotNull { stopTime ->
                stopTime.toDomain(nowSeconds)
            }

            // Prefer upcoming arrivals; fallback to all arrivals if none are in the future (e.g. test data)
            val upcomingArrivals = allArrivals.filter { it.etaMinutes >= 0 }
            val finalArrivals = (upcomingArrivals.ifEmpty { allArrivals })
                .sortedBy { it.etaMinutes }
                .map { if (it.etaMinutes < 0) it.copy(etaMinutes = 0) else it }

            StopArrival(
                stopId = stopId,
                routeId = route.routeId,
                routeShortName = route.routeShortName.orEmpty(),
                routeLongName = route.routeLongName.orEmpty(),
                routeColor = route.routeColor,
                routeTextColor = route.routeTextColor,
                arrivals = finalArrivals,
            )
        }
    }
}

/**
 * Maps a [TransitStopDetailResponse] to its domain representation [TransitStopDetail].
 * Converts each route's stop times into sorted [VehicleArrival] entries.
 */
fun TransitStopDetailResponse.toDomain(): TransitStopDetail {
    val nowSeconds = LocalTime.now().toSecondOfDay()

    val arrivals = routes.map { route ->
        val allArrivals = route.stopTimeResponses.mapNotNull { stopTime ->
            stopTime.toDomain(nowSeconds)
        }
        val upcomingArrivals = allArrivals.filter { it.etaMinutes >= 0 }
        val finalArrivals = (upcomingArrivals.ifEmpty { allArrivals })
            .sortedBy { it.etaMinutes }
            .map { if (it.etaMinutes < 0) it.copy(etaMinutes = 0) else it }

        StopArrival(
            stopId = id,
            routeId = route.routeId,
            routeShortName = route.routeShortName.orEmpty(),
            routeLongName = route.routeLongName.orEmpty(),
            routeColor = route.routeColor,
            routeTextColor = route.routeTextColor,
            arrivals = finalArrivals,
        )
    }

    return TransitStopDetail(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        type = type,
        routeArrivals = arrivals,
    )
}

fun StopTimeResponse.toDomain(nowSeconds: Int = LocalTime.now().toSecondOfDay()): VehicleArrival? {
    return try {
        val parts = arrivalTime.trim().split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val seconds = parts.getOrNull(2)?.toIntOrNull() ?: 0

        val arrivalSeconds = hours * 3600 + minutes * 60 + seconds
        var diffMinutes = (arrivalSeconds - nowSeconds) / 60

        // Handle midnight cross-over (e.g. current time 23:30, arrival time 00:30 next day)
        if (diffMinutes < -720) {
            diffMinutes += 24 * 60
        }

        val formattedTime = String.format(Locale.ROOT, "%02d:%02d", hours % 24, minutes)
        VehicleArrival(
            etaMinutes = diffMinutes,
            arrivalTime = formattedTime,
        )
    } catch (_: Exception) {
        null
    }
}
