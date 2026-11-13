package com.vie.mit.common.screenbottomsheet

/**
 * Enum định nghĩa các trạng thái hiển thị của Custom Bottom Sheet.
 *
 * [Hidden] Ẩn hoàn toàn khỏi màn hình.
 * [Peeked] Chỉ hiển thị một phần nhỏ ở cạnh dưới màn hình,
 *          cho phép người dùng kéo lên để mở rộng.
 * [PartiallyExpanded] Hiển thị khoảng 65% chiều cao màn hình.
 * [Expanded] Mở rộng tối đa, chiếm toàn bộ không gian hiển thị
 *            được thiết kế cho Bottom Sheet.
 */
enum class CustomBottomSheetValue {
    Hidden, Peeked, PartiallyExpanded, Expanded
}
