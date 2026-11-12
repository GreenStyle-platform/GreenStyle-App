package com.vie.mit.green.ui.component.bottomsheet

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vie.mit.common.theme.AppTheme

/**
 * Reusable Common Bottom Sheet component wrapping [BottomSheetScaffold].
 * Allows flexible customization of sheet height, shape, colors, content, and scaffold state.
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
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = sheetPeekHeight,
        sheetShape = sheetShape,
        sheetContainerColor = sheetContainerColor,
        sheetContentColor = sheetContentColor,
        sheetContent = sheetContent,
        modifier = modifier.fillMaxSize(),
        content = content,
    )
}
