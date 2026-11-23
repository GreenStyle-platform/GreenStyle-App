package com.vie.mit.green.ui.home

import com.vie.mit.green.ui.home.container.clearTransitLoading
import com.vie.mit.green.ui.home.homesheet.HomeSheetUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeContainerViewModelLoadStateTest {
    @Test
    fun clearTransitLoading_marksCancelledLoadAsNotLoadingWithoutChangingError() {
        val state = HomeSheetUiState(
            isLoadingTransit = true,
            errorMessage = "Existing error",
        )
        val clearedState = state.clearTransitLoading()

        assertFalse(clearedState.isLoadingTransit)
        assertEquals("Existing error", clearedState.errorMessage)
    }
}
