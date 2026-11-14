package com.vie.mit.common.screenbottomsheet

import androidx.compose.runtime.Immutable

/**
 * Cấu hình tỷ lệ chiều cao và giới hạn hiệu ứng cho Bottom Sheet.
 *
 * @property peekedRatio Tỷ lệ chiều cao khi ở trạng thái [CustomBottomSheetValue.Peeked].
 * @property partiallyExpandedRatio Tỷ lệ chiều cao khi ở trạng thái [CustomBottomSheetValue.PartiallyExpanded].
 * @property mapScaleLimitRatio Tỷ lệ chiều cao ngưỡng mà từ đó -> [peekedRatio] nội dung nền (bản đồ) bắt đầu co giãn (scale), nếu heightFraction > mapScaleLimitRatio thì map không thu nhỏ mà cho bottomSheet trượt lên.
 * @property expandedRatio Tỷ lệ chiều cao khi ở trạng thái [CustomBottomSheetValue.Expanded].
 */
@Immutable
data class BottomSheetConfig(
    val peekedRatio: Float = 0.2f,
    val partiallyExpandedRatio: Float = 0.65f,
    val expandedRatio: Float = 0.95f
)