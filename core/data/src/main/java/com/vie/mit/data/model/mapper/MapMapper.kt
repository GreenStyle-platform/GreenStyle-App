package com.vie.mit.data.model.mapper

import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.domain.map.AutosuggestPlace
import com.vie.mit.data.model.domain.map.BusRoute
import com.vie.mit.data.model.domain.map.Location
import com.vie.mit.data.model.domain.map.PublicTransportSuggestion
import com.vie.mit.data.model.domain.map.RouteCoordinate
import com.vie.mit.data.model.domain.map.RouteLeg
import com.vie.mit.data.model.domain.map.RouteLegPlace
import com.vie.mit.data.model.domain.map.RouteOption
import com.vie.mit.data.model.domain.map.RouteTrip
import com.vie.mit.data.model.domain.map.ShapePoint
import com.vie.mit.data.model.domain.map.StopArrival
import com.vie.mit.data.model.domain.map.TransitStopDetail
import com.vie.mit.data.model.domain.map.TransitStopMarker
import com.vie.mit.data.model.domain.map.VehicleArrival
import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.response.map.BusRouteResponse
import com.vie.mit.data.model.remote.response.map.HereAutocompleteItemResponse
import com.vie.mit.data.model.remote.response.map.HereAutosuggestItemResponse
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.RouteArrivalResponse
import com.vie.mit.data.model.remote.response.map.RouteTripResponse
import com.vie.mit.data.model.remote.response.map.ShapePointResponse
import com.vie.mit.data.model.remote.response.map.StopTimeResponse
import com.vie.mit.data.model.remote.response.map.TransitItineraryResponse
import com.vie.mit.data.model.remote.response.map.TransitLegResponse
import com.vie.mit.data.model.remote.response.map.TransitPlaceResponse
import com.vie.mit.data.model.remote.response.map.TransitRouteSearchResponse
import com.vie.mit.data.model.remote.response.map.TransitStopDetailResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import com.vie.mit.data.model.remote.response.map.TripStopResponse
import com.vie.mit.data.util.PolylineDecoder
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
            stopId = resolvedId,
            routeId = route.routeId,
            routeShortName = route.routeShortName.orEmpty(),
            routeLongName = route.routeLongName.orEmpty(),
            routeColor = route.routeColor,
            routeTextColor = route.routeTextColor,
            arrivals = finalArrivals,
        )
    }

    return TransitStopDetail(
        id = resolvedId,
        name = resolvedName,
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

fun RouteTripResponse.toDomain(): RouteTrip = RouteTrip(
    tripId = tripId,
    tripHeadsign = tripHeadsign.orEmpty(),
    directionId = directionId ?: 0,
    serviceId = serviceId.orEmpty(),
    shapeId = shapeId,
    routeId = routeId.orEmpty(),
    routeShortName = routeShortName.orEmpty(),
    routeLongName = routeLongName.orEmpty(),
    routeType = routeType ?: 3,
)

fun ShapePointResponse.toDomain(): ShapePoint? {
    val lat = shapePtLat ?: return null
    val lon = shapePtLon ?: return null
    return ShapePoint(
        shapeId = shapeId,
        sequence = shapePtSequence ?: 0,
        latitude = lat,
        longitude = lon,
        distTraveled = shapeDistTraveled,
    )
}

fun TripStopResponse.toDomain(): TransitStopMarker = TransitStopMarker(
    id = id,
    name = name.orEmpty(),
    latitude = latitude,
    longitude = longitude,
)

fun HereAutosuggestItemResponse.toDomain(): AutosuggestPlace {
    val addrLabel = address?.label ?: title.orEmpty()
    return AutosuggestPlace(
        id = id ?: title.orEmpty(),
        title = title ?: addrLabel,
        address = addrLabel,
        latitude = position?.lat,
        longitude = position?.lng,
        distance = distance,
        highlights = emptyList(),
    )
}

fun HereAutocompleteItemResponse.toDomain(): AutosuggestPlace {
    val addrLabel = address?.label ?: title.orEmpty()
    val highlightRanges = highlights?.title?.map { it.start to it.end }
        ?: highlights?.address?.label?.map { it.start to it.end }
        ?: emptyList()
    return AutosuggestPlace(
        id = id ?: title.orEmpty(),
        title = title ?: addrLabel,
        address = addrLabel,
        latitude = null,
        longitude = null,
        distance = distance,
        highlights = highlightRanges,
    )
}

fun TransitRouteSearchResponse.toDomain(): List<RouteOption> {
    if (itineraries.isEmpty()) return emptyList()
    val minDurationSeconds = itineraries.minOf { it.durationSeconds }
    val minWalkSeconds = itineraries.minOf { it.walkSeconds }
    val maxWalkSeconds = itineraries.maxOf { it.walkSeconds }
    val fastestIndex = itineraries.indexOfFirst { it.durationSeconds == minDurationSeconds }
    val leastWalkIndex = if (itineraries.size > 1 && minWalkSeconds < maxWalkSeconds) {
        itineraries.indexOfFirst { it.walkSeconds == minWalkSeconds }
    } else {
        -1
    }

    return itineraries.mapIndexed { index, itinerary ->
        itinerary.toDomain(
            index = index,
            isFastest = index == fastestIndex,
            isLeastWalk = index == leastWalkIndex,
        )
    }
}

fun TransitItineraryResponse.toDomain(
    index: Int = 0,
    isFastest: Boolean = false,
    isLeastWalk: Boolean = false,
): RouteOption {
    val domainLegs = legs.map { it.toDomain() }
    val transitLegs = domainLegs.filter { it.mode.isTransit }
    val distinctModes = domainLegs.map { it.mode }.toSet().ifEmpty { setOf(TransitMode.WALK) }
    val primaryMode = when {
        transitLegs.isNotEmpty() -> transitLegs.first().mode
        TransitMode.BIKE in distinctModes -> TransitMode.BIKE
        else -> TransitMode.WALK
    }
    val resolvedTitle = if (transitLegs.isNotEmpty()) {
        transitLegs.joinToString(separator = " → ") { leg ->
            val shortName = leg.routeShortName?.trim().orEmpty()
            val modeLabel = leg.mode.toVietnameseLabel()
            when {
                shortName.isNotEmpty() -> "$modeLabel $shortName"
                !leg.routeLongName.isNullOrBlank() -> leg.routeLongName.trim()
                else -> modeLabel
            }
        }
    } else {
        primaryMode.toVietnameseLabel()
    }
    val totalDurationMinutes = if (durationSeconds <= 0) {
        0
    } else {
        ((durationSeconds + 30) / 60).coerceAtLeast(1)
    }
    val walkMinutes = if (walkSeconds <= 0) {
        0
    } else {
        ((walkSeconds + 30) / 60).coerceAtLeast(1)
    }
    val descriptionParts = mutableListOf<String>()
    if (transitLegs.isNotEmpty()) {
        if (transfers <= 0) {
            descriptionParts.add("Đi thẳng")
        } else {
            descriptionParts.add("$transfers lần chuyển tuyến")
        }
    }
    if (walkMinutes > 0) {
        descriptionParts.add("Đi bộ $walkMinutes phút")
    }
    val resolvedDescription =
        descriptionParts.joinToString(separator = " • ").ifBlank { primaryMode.toVietnameseLabel() }
    val tags = mutableListOf<String>()
    if (isFastest) {
        tags.add("Nhanh nhất")
    }
    if (transitLegs.isNotEmpty() && transfers == 0) {
        tags.add("Đi thẳng")
    }
    if (isLeastWalk) {
        tags.add("Ít đi bộ nhất")
    }

    return RouteOption(
        id = id.ifBlank { "itinerary-$index" },
        title = resolvedTitle,
        description = resolvedDescription,
        durationSeconds = durationSeconds,
        durationMinutes = totalDurationMinutes,
        walkSeconds = walkSeconds,
        transfers = transfers,
        startTimeMillis = startTime?.toLongOrNull(),
        endTimeMillis = endTime?.toLongOrNull(),
        costVnd = null,
        transportType = primaryMode,
        transportTypes = distinctModes,
        legs = domainLegs,
        tags = tags,
    )
}

fun TransitLegResponse.toDomain(): RouteLeg {
    val fromPlace = from?.toDomain()
    val toPlace = to?.toDomain()
    val decodedPoints = PolylineDecoder.decode(geometry).ifEmpty {
        buildList {
            if (fromPlace != null) add(RouteCoordinate(fromPlace.latitude, fromPlace.longitude))
            if (toPlace != null) add(RouteCoordinate(toPlace.latitude, toPlace.longitude))
        }
    }
    return RouteLeg(
        mode = TransitMode.fromOtpMode(mode),
        durationSeconds = durationSeconds,
        startTimeMillis = startTime?.toLongOrNull(),
        endTimeMillis = endTime?.toLongOrNull(),
        from = fromPlace,
        to = toPlace,
        encodedGeometry = geometry,
        points = decodedPoints,
        headsign = headsign,
        routeId = routeId,
        routeShortName = routeShortName,
        routeLongName = routeLongName,
    )
}

fun TransitPlaceResponse.toDomain(): RouteLegPlace = RouteLegPlace(
    id = id,
    name = name.orEmpty(),
    latitude = latitude,
    longitude = longitude,
    arrivalTimeMillis = arrivalTime?.toLongOrNull(),
    departureTimeMillis = departureTime?.toLongOrNull(),
)

private fun TransitMode.toVietnameseLabel(): String = when (this) {
    TransitMode.BUS, TransitMode.TROLLEYBUS -> "Xe buýt"
    TransitMode.SUBWAY, TransitMode.MONORAIL -> "Metro"
    TransitMode.RAIL -> "Tàu hỏa"
    TransitMode.TRAM, TransitMode.CABLE_TRAM -> "Tàu điện"
    TransitMode.FERRY -> "Phà"
    TransitMode.AERIAL_LIFT -> "Cáp treo"
    TransitMode.BIKE -> "Xe đạp"
    TransitMode.WALK -> "Đi bộ"
}

