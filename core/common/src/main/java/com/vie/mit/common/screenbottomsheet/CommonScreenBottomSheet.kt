package com.vie.mit.common.screenbottomsheet

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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
    
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val totalHeightPx = with(density) { maxHeight.toPx() }

        // Configure anchors whenever totalHeightPx or config changes
        SideEffect {
            initState.updateAnchors(totalHeightPx, config)
        }
        // NestedScrollConnection liên kết giữa scrollable content bên trong và BottomSheet
        val nestedScrollConnection = remember(initState.anchoredState) {
            object : NestedScrollConnection {
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

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val delta = available.y
                    // Khi kéo ngón tay DOWN (delta > 0) và danh sách bên trong đã cuộn lên top: kéo sheet thu nhỏ lại
                    return if (delta > 0f) {
                        val consumedDelta = initState.anchoredState.dispatchRawDelta(delta)
                        Offset(0f, consumedDelta)
                    } else {
                        Offset.Zero
                    }
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val toFling = available.y
                    val currentOffset = initState.offset
                    val minAnchor = initState.anchoredState.anchors.minPosition()
                    return if (toFling < 0f && currentOffset > minAnchor) {
                        initState.expand()
                        available
                    } else {
                        Velocity.Zero
                    }
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    val toFling = available.y
                    return if (toFling > 0f) {
                        initState.partialExpand()
                        available
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
                    .offset {
                        val offset = initState.offset
                        val y = if (offset.isNaN()) totalHeightPx.roundToInt() else offset.roundToInt()
                        IntOffset(0, y)
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
