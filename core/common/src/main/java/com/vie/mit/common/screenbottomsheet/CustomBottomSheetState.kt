package com.vie.mit.common.screenbottomsheet

import androidx.annotation.FloatRange
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Phuccdr: Lớp quản lý trạng thái (State Holder) của Custom Bottom Sheet.
 * Quản lý trạng thái ẩn/hiện, vị trí vuốt kéo và hỗ trợ cấu hình chiều cao khi mở rộng.
 *
 * @param initialValue Trạng thái ban đầu của Bottom Sheet.
 * @param expandedHeightFraction Tỷ lệ chiều cao của Bottom Sheet khi mở rộng (Expanded) so với chiều cao màn hình (từ 0.0f đến 1.0f, mặc định là 1.0f tức 100%).
 */
@Stable
class CustomBottomSheetState(
    initialValue: CustomBottomSheetValue,
    @get:FloatRange(from = 0.0, to = 1.0) val expandedHeightFraction: Float = 1.0f,
) {
    internal val anchoredState = AnchoredDraggableState(
        initialValue = initialValue
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
            val partialOffset = anchoredState.anchors.positionOf(CustomBottomSheetValue.PartiallyExpanded)
            val peekOffset = anchoredState.anchors.positionOf(CustomBottomSheetValue.Peeked)
            if (partialOffset.isNaN() || peekOffset.isNaN()) return 0f
            val currentOffset = offset
            if (currentOffset.isNaN() || currentOffset <= partialOffset) return 0f
            val range = peekOffset - partialOffset
            return if (range > 0f) ((currentOffset - partialOffset) / range).coerceIn(0f, 1f) else 0f
        }

    /**
     * Configures the draggable anchors for the three sheet states based on total screen height.
     *
     * @param totalHeightPx Total available height in pixels.
     * @param config Configuration defining ratios for the sheet positions.
     */
    fun updateAnchors(totalHeightPx: Float, config: BottomSheetConfig) {
        this.totalHeightPx = totalHeightPx
        val newAnchors = DraggableAnchors {
            CustomBottomSheetValue.Hidden at totalHeightPx
            CustomBottomSheetValue.Peeked at totalHeightPx * (1f - config.minSheetRatio)
            CustomBottomSheetValue.PartiallyExpanded at totalHeightPx * (1f - config.initialPeekRatio)
            CustomBottomSheetValue.Expanded at totalHeightPx * (1f - config.maxExpandedRatio)
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
        anchoredState.animateTo(CustomBottomSheetValue.Hidden)
    }
}

/**
 * Phuccdr: Tạo và lưu giữ (remember) trạng thái [CustomBottomSheetState] qua các lần Recomposition.
 *
 * @param initialValue Trạng thái ban đầu của Bottom Sheet (mặc định là [CustomBottomSheetValue.PartiallyExpanded]).
 * @param expandedHeightFraction Tỷ lệ chiều cao khi mở rộng so với chiều cao màn hình (từ 0.0f đến 1.0f, mặc định 1.0f).
 */
@Composable
fun rememberCustomBottomSheetState(
    initialValue: CustomBottomSheetValue = CustomBottomSheetValue.PartiallyExpanded,
    @FloatRange(from = 0.0, to = 1.0) expandedHeightFraction: Float = 1.0f,
): CustomBottomSheetState {
    return remember(initialValue, expandedHeightFraction) {
        CustomBottomSheetState(
            initialValue = initialValue,
            expandedHeightFraction = expandedHeightFraction.coerceIn(0f, 1f),
        )
    }
}
