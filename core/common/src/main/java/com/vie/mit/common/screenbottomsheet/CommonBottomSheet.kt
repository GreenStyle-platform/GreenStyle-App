package com.vie.mit.common.screenbottomsheet

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vie.mit.common.theme.AppTheme

/**
 * Phuccdr: Thành phần Common Bottom Sheet dùng chung wrapping [BottomSheetScaffold].
 * Cho phép tùy chỉnh linh hoạt chiều cao hé mở (peek height), hình dạng, màu sắc, nội dung sheet và trạng thái scaffold.
 * Truyền chiều cao thực tế của sheet và tiến trình mở rộng vào lambda [content].
 *
 * @param sheetContent Composable chứa nội dung hiển thị bên trong Bottom Sheet.
 * @param modifier Modifier tùy chỉnh layout cho component.
 * @param sheetPeekHeight Chiều cao hé mở tối thiểu của Bottom Sheet khi ở trạng thái PartiallyExpanded (mặc định 240.dp).
 * @param sheetShape Hình dạng (bo góc) của Bottom Sheet.
 * @param sheetContainerColor Màu nền của Bottom Sheet.
 * @param sheetContentColor Màu sắc của nội dung bên trong Bottom Sheet.
 * @param scaffoldState Trạng thái điều khiển scaffold của Bottom Sheet.
 * @param content Lambda chứa giao diện màn hình chính bên dưới sheet, nhận vào innerPadding, sheetHeight động và expandProgress từ 0.0f đến 1.0f.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonBottomSheet(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetPeekHeight: Dp = 240.dp,
    sheetShape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    sheetContainerColor: Color = AppTheme.colors.primary,
    sheetContentColor: Color = Color.White,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    ),
    content: @Composable (innerPadding: PaddingValues, sheetHeight: Dp, expandProgress: Float) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val currentSheetHeightPx = remember(scaffoldState.bottomSheetState, totalHeightPx) {
            derivedStateOf {
                val offset =
                    runCatching { scaffoldState.bottomSheetState.requireOffset() }.getOrNull()
                if (offset != null && totalHeightPx > 0f) {
                    (totalHeightPx - offset).coerceIn(0f, totalHeightPx)
                } else {
                    with(density) { sheetPeekHeight.toPx() }
                }
            }
        }.value
        val currentSheetHeight = with(density) { currentSheetHeightPx.toDp() }
        val expandProgress = remember(currentSheetHeightPx, totalHeightPx) {
            derivedStateOf {
                val peekPx = with(density) { sheetPeekHeight.toPx() }
                val maxDragRange = totalHeightPx - peekPx
                if (maxDragRange > 0f) {
                    ((currentSheetHeightPx - peekPx) / maxDragRange).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
        }.value

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = sheetPeekHeight,
            sheetShape = sheetShape,
            sheetContainerColor = sheetContainerColor,
            sheetContentColor = sheetContentColor,
            sheetContent = sheetContent,
            modifier = Modifier.fillMaxSize(),
            content = { innerPadding ->
                content(innerPadding, currentSheetHeight, expandProgress)
            },
        )
    }
}

/**
 * Phuccdr: Overload hỗ trợ tương thích ngược cho [CommonBottomSheet], chấp nhận lambda [content] đơn giản chỉ nhận [PaddingValues].
 *
 * @param sheetContent Composable chứa nội dung hiển thị bên trong Bottom Sheet.
 * @param modifier Modifier tùy chỉnh layout cho component.
 * @param sheetPeekHeight Chiều cao hé mở tối thiểu của Bottom Sheet (mặc định 240.dp).
 * @param sheetShape Hình dạng bo góc của Bottom Sheet.
 * @param sheetContainerColor Màu nền của Bottom Sheet.
 * @param sheetContentColor Màu sắc của nội dung bên trong Bottom Sheet.
 * @param scaffoldState Trạng thái điều khiển scaffold của Bottom Sheet.
 * @param content Lambda chứa nội dung màn hình chính nhận [PaddingValues].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonBottomSheet(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetPeekHeight: Dp = 240.dp,
    sheetShape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    sheetContainerColor: Color = AppTheme.colors.primary,
    sheetContentColor: Color = Color.White,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    ),
    content: @Composable (PaddingValues) -> Unit,
) {
    CommonBottomSheet(
        sheetContent = sheetContent,
        modifier = modifier,
        sheetPeekHeight = sheetPeekHeight,
        sheetShape = sheetShape,
        sheetContainerColor = sheetContainerColor,
        sheetContentColor = sheetContentColor,
        scaffoldState = scaffoldState,
        content = { innerPadding, _, _ ->
            content(innerPadding)
        },
    )
}
