package com.vie.mit.mapbox.util

import com.vie.mit.mapbox.model.TransitViewportBounds

/**
 * Checks if this [TransitViewportBounds] is completely contained inside [container].
 */
fun TransitViewportBounds.isInside(container: TransitViewportBounds): Boolean =
    minLatitude >= container.minLatitude &&
            maxLatitude <= container.maxLatitude &&
            minLongitude >= container.minLongitude &&
            maxLongitude <= container.maxLongitude
