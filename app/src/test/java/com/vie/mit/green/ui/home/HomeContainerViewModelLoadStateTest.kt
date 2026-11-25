package com.vie.mit.green.ui.home

import com.vie.mit.green.ui.home.common.formatDistance
import com.vie.mit.green.ui.home.container.HomeContainerUiState
import com.vie.mit.green.ui.home.container.clearTransitLoading
import com.vie.mit.mapbox.composable.formatRadiusDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeContainerViewModelLoadStateTest {
    @Test
    fun clearTransitLoading_marksCancelledLoadAsNotLoadingWithoutChangingError() {
        val state = HomeContainerUiState(
            isLoadingTransit = true,
            errorMessage = "Existing error",
        )
        val clearedState = state.clearTransitLoading()

        assertFalse(clearedState.isLoadingTransit)
        assertEquals("Existing error", clearedState.errorMessage)
    }

    @Test
    fun formatDistance_matchesFormatRadiusDistanceAcrossRoundingBoundary() {
        assertEquals(formatRadiusDistance(0.9997), formatDistance(999.7f))
        assertEquals(formatRadiusDistance(0.45), formatDistance(450f))
        assertEquals(formatRadiusDistance(1.25), formatDistance(1250f))
    }
}
