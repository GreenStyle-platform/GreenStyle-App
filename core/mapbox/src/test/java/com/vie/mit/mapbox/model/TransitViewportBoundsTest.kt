package com.vie.mit.mapbox.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TransitViewportBoundsTest {

    @Test
    fun bufferedRequestBounds_expandsAndClampsLatitude() {
        val viewport = TransitMapViewportInfo(
            minLatitude = 89.0,
            minLongitude = 105.0,
            maxLatitude = 90.0,
            maxLongitude = 106.0,
            zoom = 14.0,
            centerLatitude = 89.5,
            centerLongitude = 105.5,
        )

        val bounds = viewport.bufferedRequestBounds()

        assertEquals(1, bounds.size)
        assertEquals(88.65, bounds.single().minLatitude, 1e-9)
        assertEquals(90.0, bounds.single().maxLatitude, 1e-9)
        assertEquals(104.65, bounds.single().minLongitude, 1e-9)
        assertEquals(106.35, bounds.single().maxLongitude, 1e-9)
    }

    @Test
    fun bufferedRequestBounds_splitsAntimeridianViewportIntoValidRequests() {
        val viewport = TransitMapViewportInfo(
            minLatitude = 20.0,
            minLongitude = 179.0,
            maxLatitude = 21.0,
            maxLongitude = -179.0,
            zoom = 14.0,
            centerLatitude = 20.5,
            centerLongitude = 180.0,
        )

        val bounds = viewport.bufferedRequestBounds()

        assertEquals(2, bounds.size)
        assertEquals(178.3, bounds[0].minLongitude, 1e-9)
        assertEquals(180.0, bounds[0].maxLongitude, 1e-9)
        assertEquals(-180.0, bounds[1].minLongitude, 1e-9)
        assertEquals(-178.3, bounds[1].maxLongitude, 1e-9)
    }
}
