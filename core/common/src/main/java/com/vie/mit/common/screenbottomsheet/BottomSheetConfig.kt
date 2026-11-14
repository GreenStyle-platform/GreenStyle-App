package com.vie.mit.common.screenbottomsheet

import androidx.compose.runtime.Immutable

/**
 * Cấu hình tỷ lệ chiều cao và giới hạn hiệu ứng cho Bottom Sheet.
 *
 * @property initialPeekRatio Tỷ lệ chiều cao hiển thị ban đầu của Bottom Sheet so với tổng chiều cao màn hình.
 * @property minSheetRatio Tỷ lệ chiều cao tối thiểu khi vuốt thu nhỏ Bottom Sheet (trạng thái collapsed).
 * @property mapScaleLimitRatio Tỷ lệ chiều cao ngưỡng mà từ đó -> [minSheetRatio] nội dung nền (bản đồ) bắt đầu co giãn (scale), nếu heightFraction > mapScaleLimitRatio thì map không thu nhỏ mà cho bottomSheet trượt lên
 * @property maxExpandedRatio Tỷ lệ chiều cao tối đa khi mở rộng toàn bộ Bottom Sheet (trạng thái expanded).
 */
@Immutable
data class BottomSheetConfig(
    val initialPeekRatio: Float = 0.65f,
    val minSheetRatio: Float = 0.2f,
    val mapScaleLimitRatio: Float = 0.65f,
    val maxExpandedRatio: Float = 0.95f
)