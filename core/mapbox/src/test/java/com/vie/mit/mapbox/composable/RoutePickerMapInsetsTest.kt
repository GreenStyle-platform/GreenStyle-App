package com.vie.mit.mapbox.composable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePickerMapInsetsTest {

    @Test
    fun computeRouteFitEdgeInsets_clearsStatusBarBackButtonAndSheetAtPartiallyExpanded() {
        // 1080x2400 @ 3x density, statusBar = 108px, sheet = 60%
        val insets = computeRouteFitEdgeInsets(
            mapWidthPx = 1080,
            mapHeightPx = 2400,
            statusBarTopPx = 108.0,
            routeFitBottomRatio = 0.60f,
            density = 3.0f,
        )

        assertEquals(300.0, insets.top, 0.01)
        assertEquals(1536.0, insets.bottom, 0.01)
        assertEquals(120.0, insets.left, 0.01)
        assertEquals(120.0, insets.right, 0.01)
        assertTrue(insets.top + insets.bottom <= 2400.0 - 360.0)
    }

    @Test
    fun computeRouteFitEdgeInsets_expandsVisibleRouteAreaWhenSheetIsPeeked() {
        val partialInsets = computeRouteFitEdgeInsets(
            mapWidthPx = 1080,
            mapHeightPx = 2400,
            statusBarTopPx = 108.0,
            routeFitBottomRatio = 0.60f,
            density = 3.0f,
        )
        val peekedInsets = computeRouteFitEdgeInsets(
            mapWidthPx = 1080,
            mapHeightPx = 2400,
            statusBarTopPx = 108.0,
            routeFitBottomRatio = 0.25f,
            density = 3.0f,
        )

        assertEquals(696.0, peekedInsets.bottom, 0.01)
        assertTrue(peekedInsets.bottom < partialInsets.bottom)
    }

    @Test
    fun computeRouteFitEdgeInsets_clampsSafelyOnSmallScreensWithoutOverPadding() {
        val insets = computeRouteFitEdgeInsets(
            mapWidthPx = 480,
            mapHeightPx = 640,
            statusBarTopPx = 48.0,
            routeFitBottomRatio = 0.80f,
            density = 2.0f,
        )

        // Never exceeds available height minus min visible height (120dp * 2 = 240px)
        assertTrue(insets.top + insets.bottom <= 640.0 - 240.0 + 0.01)
        assertTrue(insets.left + insets.right <= 480.0 - 200.0 + 0.01)
    }
}
