package com.vie.mit.mapbox.model

data class TransitViewportBounds(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double,
)

fun TransitMapViewportInfo.bufferedRequestBounds(
    bufferFactor: Double = 0.35,
): List<TransitViewportBounds> {
    val latSpan = maxLatitude - minLatitude
    val lonSpan = if (minLongitude <= maxLongitude) {
        maxLongitude - minLongitude
    } else {
        360.0 - minLongitude + maxLongitude
    }
    val minLat = (minLatitude - latSpan * bufferFactor).coerceIn(-90.0, 90.0)
    val maxLat = (maxLatitude + latSpan * bufferFactor).coerceIn(-90.0, 90.0)
    val minLon = minLongitude - lonSpan * bufferFactor
    val maxLon = maxLongitude + lonSpan * bufferFactor

    return splitWrappedLongitudes(minLat, minLon, maxLat, maxLon)
}

private fun splitWrappedLongitudes(
    minLat: Double,
    minLon: Double,
    maxLat: Double,
    maxLon: Double,
): List<TransitViewportBounds> {
    if (maxLon - minLon >= 360.0) {
        return listOf(TransitViewportBounds(minLat, -180.0, maxLat, 180.0))
    }

    val normalizedMinLon = normalizeLongitude(minLon)
    val normalizedMaxLon = normalizeLongitude(maxLon)
    return if (normalizedMinLon <= normalizedMaxLon && minLon <= maxLon) {
        listOf(TransitViewportBounds(minLat, normalizedMinLon, maxLat, normalizedMaxLon))
    } else {
        listOf(
            TransitViewportBounds(minLat, normalizedMinLon, maxLat, 180.0),
            TransitViewportBounds(minLat, -180.0, maxLat, normalizedMaxLon),
        )
    }
}

private fun normalizeLongitude(longitude: Double): Double {
    val normalized = ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
    return if (normalized == -180.0 && longitude > 0.0) 180.0 else normalized
}
