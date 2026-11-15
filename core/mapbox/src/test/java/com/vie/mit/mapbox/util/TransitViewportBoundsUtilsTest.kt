package com.vie.mit.mapbox.util

import com.vie.mit.mapbox.model.TransitViewportBounds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitViewportBoundsUtilsTest {

    @Test
    fun isInside_returnsTrue_whenBoundsIsInsideContainer() {
        val inner = TransitViewportBounds(
            minLatitude = 10.5,
            minLongitude = 105.5,
            maxLatitude = 10.8,
            maxLongitude = 105.8,
        )
        val outer = TransitViewportBounds(
            minLatitude = 10.0,
            minLongitude = 105.0,
            maxLatitude = 11.0,
            maxLongitude = 106.0,
        )

        assertTrue(inner.isInside(outer))
    }

    @Test
    fun isInside_returnsFalse_whenBoundsExceedsContainer() {
        val inner = TransitViewportBounds(
            minLatitude = 9.5,
            minLongitude = 105.5,
            maxLatitude = 10.8,
            maxLongitude = 105.8,
        )
        val outer = TransitViewportBounds(
            minLatitude = 10.0,
            minLongitude = 105.0,
            maxLatitude = 11.0,
            maxLongitude = 106.0,
        )

        assertFalse(inner.isInside(outer))
    }
}
