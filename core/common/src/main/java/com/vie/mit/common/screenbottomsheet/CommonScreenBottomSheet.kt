package com.vie.mit.common.screenbottomsheet

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.vie.mit.common.theme.AppTheme
import kotlin.math.roundToInt

/**
 * Phuccdr: Composable hiển thị màn hình Bottom Sheet chính dựa trên trạng thái [CustomBottomSheetState].
 *
 * @param initState Trạng thái khởi tạo hoặc quản lý Bottom Sheet.
 * @param config Cấu hình tỷ lệ chiều cao sheet.
 * @param backgroundContent Nội dung nền (ví dụ: bản đồ) nhận [MapTransform] để co giãn theo sheet.
 * @param sheetContent Nội dung bên trong Bottom Sheet.
 */
@Composable
fun CommonScreenBottomSheet(
    initState: CustomBottomSheetState = rememberCustomBottomSheetState(),
    config: BottomSheetConfig = BottomSheetConfig(),
    backgroundContent: @Composable (mapTransform: MapTransform) -> Unit,
    sheetContent: @Composable () -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val totalHeightPx = with(density) { maxHeight.toPx() }

        // Cập nhật anchors ngay trong lúc Composition để offset khởi tạo đúng trước bước Layout/Placement
        if (totalHeightPx > 0f) {
            initState.updateAnchors(totalHeightPx, config)
        }
        // NestedScrollConnection liên kết giữa scrollable content bên trong và BottomSheet.
        // Điều phối cử chỉ kéo (scroll) và vuốt nhanh (fling) giữa danh sách bên trong (LazyColumn/Scrollable) và BottomSheet.
        val nestedScrollConnection = remember(initState.anchoredState) {
            object : NestedScrollConnection {

                /**
                 * Tiêu thụ độ dời cuộn (scroll delta) TRƯỚC KHI (Pre) danh sách con bên trong tiêu thụ.
                 *
                 * - Trường hợp kéo lên (delta < 0f): Nếu BottomSheet chưa mở rộng hoàn toàn (chưa đạt anchor Expanded),
                 *   hàm sẽ ưu tiên kéo mở rộng BottomSheet lên trước. Danh sách con chỉ cuộn khi Sheet đã mở hết cỡ.
                 * - Trường hợp kéo xuống (delta >= 0f): Trả về Offset.Zero để nhường quyền cuộn cho danh sách con trước.
                 */
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    // Khi kéo ngón tay UP (delta < 0) và sheet chưa mở hết tối đa: ưu tiên kéo sheet lên
                    return if (delta < 0f) {
                        val consumed = initState.anchoredState.dispatchRawDelta(delta)
                        Offset(0f, consumed)
                    } else {
                        Offset.Zero
                    }
                }

                /**
                 * Tiêu thụ độ dời cuộn (scroll delta) SAU KHI (Post) danh sách con bên trong đã tiêu thụ xong.
                 *
                 * - Trường hợp kéo xuống (delta > 0f): Khi danh sách con đã cuộn lên trên cùng (không thể cuộn thêm),
                 *   lượng delta dư thừa (available.y) sẽ được dùng để kéo thu nhỏ BottomSheet xuống.
                 * - Sử dụng hệ số 0.6f để giảm tốc / tạo lực cản (friction) giúp hiệu ứng cuộn mượt mà tự nhiên hơn.
                 */
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val delta = available.y
                    // Khi kéo ngón tay DOWN (delta > 0) và danh sách bên trong đã cuộn lên top: kéo sheet thu nhỏ lại
                    return if (delta > 0f) {
                        val consumedDelta = initState.anchoredState.dispatchRawDelta(delta) * 0.6f
                        Offset(0f, consumedDelta)
                    } else {
                        Offset.Zero
                    }
                }

                /**
                 * Tiêu thụ vận tốc cuộn đà (fling velocity) TRƯỚC KHI (Pre) danh sách con thực hiện hiệu ứng fling.
                 *
                 * - Trường hợp vuốt nhanh lên trên (toFling < 0f): Nếu BottomSheet chưa đạt vị trí trần (minPosition / Expanded),
                 *   BottomSheet sẽ bắt lấy vận tốc này và gọi `initState.settle` để chạy animation hít (snap) Sheet lên trạng thái tiếp theo.
                 * - Trả về lượng vận tốc đã tiêu thụ để danh sách con không cuộn đà đè lên trong lúc Sheet đang di chuyển.
                 */
                override suspend fun onPreFling(available: Velocity): Velocity {
                    val toFling = available.y
                    val currentOffset = initState.offset
                    val minAnchor = initState.anchoredState.anchors.minPosition()
                    return if (toFling < 0f && currentOffset > minAnchor) {
                        val consumedY = initState.settle(toFling)
                        Velocity(0f, consumedY)
                    } else {
                        Velocity.Zero
                    }
                }

                /**
                 * Tiêu thụ vận tốc cuộn đà (fling velocity) SAU KHI (Post) danh sách con đã fling hết hoặc chạm đỉnh.
                 *
                 * - Trường hợp vuốt nhanh xuống dưới (toFling > 0f): Khi danh sách đã ở đỉnh và không thể fling tiếp,
                 *   vận tốc còn lại (available.y) sẽ chuyển cho BottomSheet để gọi `initState.settle` đưa Sheet về trạng thái thu nhỏ/ẩn.
                 * - Nhân hệ số 0.6f để giảm chấn vận tốc fling xuống.
                 */
                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    val toFling = available.y
                    return if (toFling > 0f) {
                        val consumedY = initState.settle(toFling) * 0.6f
                        Velocity(0f, consumedY)
                    } else {
                        Velocity.Zero
                    }
                }
            }
        }

        // Background content (e.g. map) — full screen at background, zero layout resize for max performance
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
        ) {
            backgroundContent(
                MapTransform(
                    scale = 1f,
                    translationY = 0f,
                    heightFraction = initState.sheetFraction,
                )
            )
        }

        // Sheet container with anchored draggable and nested scroll connection
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val currentOffset = initState.offset
                        val y = if (currentOffset.isNaN()) totalHeightPx.roundToInt() else currentOffset.roundToInt()
                        val visibleHeight = (totalHeightPx - y).coerceAtLeast(0f).roundToInt()
                        val placeable = measurable.measure(
                            constraints.copy(
                                minHeight = visibleHeight,
                                maxHeight = visibleHeight
                            )
                        )
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(0, y)
                        }
                    }
                    .nestedScroll(nestedScrollConnection)
                    .anchoredDraggable(
                        state = initState.anchoredState,
                        orientation = Orientation.Vertical
                    ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = AppTheme.colors.primary,
                shadowElevation = 8.dp
            ) {
                sheetContent()
            }
        }
    }
}
