package com.vie.mit.green.ui.component.bottomsheet

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Stable
class CustomBottomSheetState(
    initialValue: CustomBottomSheetValue
) {
    internal val anchoredState = AnchoredDraggableState(
        initialValue = initialValue
    )
    val currentValue: CustomBottomSheetValue
        get() = anchoredState.currentValue
    val targetValue: CustomBottomSheetValue
        get() = anchoredState.targetValue
    val offset: Float
        get() = anchoredState.offset
    val isVisible: Boolean
        get() = currentValue != CustomBottomSheetValue.Hidden

    suspend fun expand() {
        anchoredState.animateTo(CustomBottomSheetValue.Expanded)
    }

    suspend fun partialExpand() {
        anchoredState.animateTo(
            CustomBottomSheetValue.PartiallyExpanded
        )
    }

    suspend fun hide() {
        anchoredState.animateTo(CustomBottomSheetValue.Hidden)
    }
}

@Composable
fun rememberCustomBottomSheetState(
    initialValue: CustomBottomSheetValue = CustomBottomSheetValue.PartiallyExpanded
): CustomBottomSheetState {
    return remember {
        CustomBottomSheetState(initialValue)
    }
}
