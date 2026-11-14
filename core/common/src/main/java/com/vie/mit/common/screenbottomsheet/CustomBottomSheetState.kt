package com.vie.mit.common.screenbottomsheet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Phuccdr: Lớp quản lý trạng thái (State Holder) của Custom Bottom Sheet.
 * Quản lý trạng thái ẩn/hiện, vị trí vuốt kéo và hỗ trợ cấu hình chiều cao khi mở rộng.
 *
 * @param initialValue Trạng thái ban đầu của Bottom Sheet.
 */
@Stable
class CustomBottomSheetState(
    initialValue: CustomBottomSheetValue,
    positionalThreshold: (totalDistance: Float) -> Float = { distance -> distance * 0.5f },
    velocityThreshold: () -> Float = { 125f },
    snapAnimationSpec: AnimationSpec<Float> = spring(),
    decayAnimationSpec: DecayAnimationSpec<Float> = exponentialDecay(),
    confirmValueChange: (newValue: CustomBottomSheetValue) -> Boolean = { true },
) {
    var currentConfig: BottomSheetConfig by mutableStateOf(BottomSheetConfig())
        private set
    private var isProgrammaticHide: Boolean = false

    @Suppress("DEPRECATION")
    val anchoredState = AnchoredDraggableState(
        initialValue = initialValue,
        positionalThreshold = positionalThreshold,
        velocityThreshold = velocityThreshold,
        snapAnimationSpec = snapAnimationSpec,
        decayAnimationSpec = decayAnimationSpec,
        confirmValueChange = { newValue ->
            if (newValue == CustomBottomSheetValue.Hidden && currentConfig.peekedRatio > 0f && !isProgrammaticHide) {
                false
            } else {
                confirmValueChange(newValue)
            }
        },
    )

    /** Total height in pixels, set when [updateAnchors] is called. */
    var totalHeightPx: Float = 0f
        private set
    val currentValue: CustomBottomSheetValue
        get() = anchoredState.currentValue
    val targetValue: CustomBottomSheetValue
        get() = anchoredState.targetValue
    val offset: Float
        get() = anchoredState.offset
    val isVisible: Boolean
        get() = currentValue != CustomBottomSheetValue.Hidden

    /**
     * Current sheet height in pixels (distance from bottom of screen to top of sheet).
     * Guards against NaN when offset is not yet initialized.
     */
    val sheetHeightPx: Float
        get() {
            val currentOffset = offset
            return if (currentOffset.isNaN() || totalHeightPx <= 0f) 0f
            else (totalHeightPx - currentOffset).coerceAtLeast(0f)
        }

    /**
     * Height in pixels of the BottomSheet when at [CustomBottomSheetValue.PartiallyExpanded].
     */
    val partiallyExpandedHeightPx: Float
        get() {
            if (totalHeightPx <= 0f) return 0f
            val partialOffset = anchoredState.anchors.positionOf(CustomBottomSheetValue.PartiallyExpanded)
            if (partialOffset.isNaN()) return 0f
            return (totalHeightPx - partialOffset).coerceAtLeast(0f)
        }

    /**
     * Current sheet fraction (0..1) relative to the total height.
     */
    val sheetFraction: Float
        get() = if (totalHeightPx > 0f) (sheetHeightPx / totalHeightPx).coerceIn(0f, 1f) else 0f

    /**
     * Progress from PartiallyExpanded (0f) to Expanded (1f).
     */
    val expandProgress: Float
        get() {
            if (totalHeightPx <= 0f) return 0f
            val partialOffset: Float =
                anchoredState.anchors.positionOf(CustomBottomSheetValue.PartiallyExpanded)
            val expandedOffset: Float =
                anchoredState.anchors.positionOf(CustomBottomSheetValue.Expanded)
            if (partialOffset.isNaN() || expandedOffset.isNaN()) return 0f
            val range = partialOffset - expandedOffset
            if (range <= 0f) return 0f
            val currentOffset = offset
            if (currentOffset.isNaN()) return 0f
            return ((partialOffset - currentOffset) / range).coerceIn(0f, 1f)
        }

    /**
     * Visible height in pixels of the content above the BottomSheet.
     * When Hidden or offset is NaN, returns totalHeightPx (full screen).
     */
    val visibleTopHeightPx: Float
        get() {
            val currentOffset = offset
            return if (currentOffset.isNaN() || currentValue == CustomBottomSheetValue.Hidden) {
                totalHeightPx
            } else {
                currentOffset.coerceIn(0f, totalHeightPx)
            }
        }

    /**
     * Progress (0f..1f) when sheet is dragged down past PartiallyExpanded towards Peeked/Hidden.
     */
    val dragDownFromPartialProgress: Float
        get() {
            if (totalHeightPx <= 0f) return 0f
            val partialOffset =
                anchoredState.anchors.positionOf(CustomBottomSheetValue.PartiallyExpanded)
            val peekOffset = anchoredState.anchors.positionOf(CustomBottomSheetValue.Peeked)
            if (partialOffset.isNaN() || peekOffset.isNaN()) return 0f
            val currentOffset = offset
            if (currentOffset.isNaN() || currentOffset <= partialOffset) return 0f
            val range = peekOffset - partialOffset
            return if (range > 0f) ((currentOffset - partialOffset) / range).coerceIn(
                0f,
                1f
            ) else 0f
        }

    /**
     * Configures the draggable anchors for the sheet states based on total screen height.
     *
     * @param totalHeightPx Total available height in pixels.
     * @param config Configuration defining ratios for the sheet positions.
     */
    fun updateAnchors(totalHeightPx: Float, config: BottomSheetConfig) {
        if (this.totalHeightPx == totalHeightPx && this.currentConfig == config) return
        this.totalHeightPx = totalHeightPx
        this.currentConfig = config
        val newAnchors = DraggableAnchors {
//            CustomBottomSheetValue.Hidden at totalHeightPx
            CustomBottomSheetValue.Peeked at totalHeightPx * (1f - config.peekedRatio)
            CustomBottomSheetValue.PartiallyExpanded at totalHeightPx * (1f - config.partiallyExpandedRatio)
            CustomBottomSheetValue.Expanded at totalHeightPx * (1f - config.expandedRatio)
        }
        anchoredState.updateAnchors(newAnchors)
    }

    suspend fun expand() {
        anchoredState.animateTo(CustomBottomSheetValue.Expanded)
    }

    suspend fun partialExpand() {
        anchoredState.animateTo(
            CustomBottomSheetValue.PartiallyExpanded
        )
    }

    suspend fun peek() {
        anchoredState.animateTo(CustomBottomSheetValue.Peeked)
    }

    suspend fun hide() {
        isProgrammaticHide = true
        try {
            anchoredState.animateTo(CustomBottomSheetValue.Hidden)
        } finally {
            isProgrammaticHide = false
        }
    }

    suspend fun animateTo(target: CustomBottomSheetValue) {
        when (target) {
            CustomBottomSheetValue.Expanded -> expand()
            CustomBottomSheetValue.PartiallyExpanded -> partialExpand()
            CustomBottomSheetValue.Peeked -> peek()
            CustomBottomSheetValue.Hidden -> hide()
        }
    }

    @Suppress("DEPRECATION")
    suspend fun settle(velocity: Float): Float {
        if (currentConfig.peekedRatio > 0f && velocity > 0f && !isProgrammaticHide) {
            val peekPosition = anchoredState.anchors.positionOf(CustomBottomSheetValue.Peeked)
            if (!peekPosition.isNaN() && anchoredState.offset >= peekPosition) {
                anchoredState.animateTo(CustomBottomSheetValue.Peeked)
                return velocity
            }
        }
        return anchoredState.settle(velocity)
    }
}

/**
 * Phuccdr: Tạo và lưu giữ (remember) trạng thái [CustomBottomSheetState] qua các lần Recomposition.
 *
 * @param initialValue Trạng thái ban đầu của Bottom Sheet (mặc định là [CustomBottomSheetValue.PartiallyExpanded]).
 */
@Composable
fun rememberCustomBottomSheetState(
    initialValue: CustomBottomSheetValue = CustomBottomSheetValue.PartiallyExpanded,
    positionalThreshold: (totalDistance: Float) -> Float = { distance -> distance * 0.5f },
    velocityThreshold: (() -> Float)? = null,
    snapAnimationSpec: AnimationSpec<Float> = spring(),
    decayAnimationSpec: DecayAnimationSpec<Float> = exponentialDecay(),
    confirmValueChange: (newValue: CustomBottomSheetValue) -> Boolean = { true },
): CustomBottomSheetState {
    val density = LocalDensity.current
    val resolvedVelocityThreshold = velocityThreshold ?: remember(density) {
        { with(density) { 125.dp.toPx() } }
    }
    return remember(initialValue) {
        CustomBottomSheetState(
            initialValue = initialValue,
            positionalThreshold = positionalThreshold,
            velocityThreshold = resolvedVelocityThreshold,
            snapAnimationSpec = snapAnimationSpec,
            decayAnimationSpec = decayAnimationSpec,
            confirmValueChange = confirmValueChange,
        )
    }
}


