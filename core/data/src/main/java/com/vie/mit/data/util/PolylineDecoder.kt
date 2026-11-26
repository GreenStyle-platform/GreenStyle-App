package com.vie.mit.data.util

import com.vie.mit.data.model.domain.map.RouteCoordinate
import kotlin.math.pow

/**
 * Decodes Google Encoded Polyline strings (precision 5 by default, as produced by OpenTripPlanner 2)
 * into geographic coordinates.
 */
object PolylineDecoder {
    fun decode(encoded: String?, precision: Int = 5): List<RouteCoordinate> {
        if (encoded.isNullOrEmpty()) return emptyList()
        val coordinates = mutableListOf<RouteCoordinate>()
        var index = 0
        val length = encoded.length
        var lat = 0
        var lng = 0
        val factor = 10.0.pow(precision.toDouble())

        while (index < length) {
            var byteValue: Int
            var shift = 0
            var result = 0
            do {
                if (index >= length) return coordinates
                byteValue = encoded[index++].code - 63
                result = result or ((byteValue and 0x1F) shl shift)
                shift += 5
            } while (byteValue >= 0x20)
            val deltaLat = if ((result and 1) != 0) (result ushr 1).inv() else (result ushr 1)
            lat += deltaLat

            shift = 0
            result = 0
            do {
                if (index >= length) return coordinates
                byteValue = encoded[index++].code - 63
                result = result or ((byteValue and 0x1F) shl shift)
                shift += 5
            } while (byteValue >= 0x20)
            val deltaLng = if ((result and 1) != 0) (result ushr 1).inv() else (result ushr 1)
            lng += deltaLng

            coordinates.add(
                RouteCoordinate(
                    latitude = lat / factor,
                    longitude = lng / factor,
                ),
            )
        }

        return coordinates
    }
}
