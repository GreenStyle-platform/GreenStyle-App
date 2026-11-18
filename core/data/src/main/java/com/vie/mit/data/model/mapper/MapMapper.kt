package com.vie.mit.data.model.mapper

import com.vie.mit.data.model.domain.map.Location
import com.vie.mit.data.model.domain.map.PublicTransportSuggestion
import com.vie.mit.data.model.domain.map.StopArrival
import com.vie.mit.data.model.domain.map.TransitStopMarker
import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.StopArrivalResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse

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

fun StopArrivalResponse.toDomain(): StopArrival = StopArrival(
    routeId = routeId,
    routeShortName = routeShortName,
    routeLongName = routeLongName,
    headsign = headsign,
    etaMinutes = (arrivalTimeSeconds / 60).coerceAtLeast(0),
    vehicleId = vehicleId,
)
