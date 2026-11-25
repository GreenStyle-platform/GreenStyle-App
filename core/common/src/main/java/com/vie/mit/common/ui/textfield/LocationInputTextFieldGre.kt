package com.vie.mit.common.ui.textfield

import android.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.common.theme.GreenTheme

/**
 * Ô nhập liệu địa điểm dùng chung cho luồng tìm đường (Điểm đi / Điểm đến).
 *
 * - Khi [isActive] = true: nền trắng, viền 1.5dp theo [accentColor].
 * - Khi [isActive] = false: nền xám nhẹ (`AppTheme.colors.inputContainerUnfocused`), viền 1dp.
 * - Hỗ trợ [leadingIcon] (ví dụ icon GPS cho "Vị trí hiện tại"), nút xóa nhanh (X), và IME action.
 */
@Composable
fun LocationInputTextFieldGre(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    accentColor: Color = AppTheme.colors.routeStartColor,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = accentColor,
    textColor: Color = AppTheme.colors.primaryText,
    height: Dp = 52.dp,
    focusRequester: FocusRequester = remember { FocusRequester() },
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    imeAction: ImeAction = ImeAction.Search,
    onImeAction: () -> Unit = {},
    onFocused: () -> Unit = {},
    onClick: () -> Unit = {},
    onClear: () -> Unit = { onValueChange("") },
) {
    val colors = AppTheme.colors
    val containerColor by animateColorAsState(
        targetValue = if (isActive) colors.white else colors.inputContainerUnfocused,
        animationSpec = tween(durationMillis = 160),
        label = "location_input_container_color",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) accentColor else colors.inputBorderUnfocused,
        animationSpec = tween(durationMillis = 160),
        label = "location_input_border_color",
    )
    val borderWidth = if (isActive) 1.5.dp else 1.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = leadingIconTint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = interactionSource,
                textStyle = AppTheme.typography.bodyLarge.copy(color = textColor, fontSize = 16.sp),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onNext = { onImeAction() },
                    onSearch = { onImeAction() },
                    onDone = { onImeAction() },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onFocused()
                        }
                    },
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = AppTheme.typography.bodyLarge.copy(
                                color =  colors.inputPlaceholder,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                },
            )
        }

        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(20.dp)
                        .background(colors.inputBorderUnfocused, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Xóa nội dung",
                        tint = colors.secondaryText,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationInputTextFieldGrePreview() {
    GreenTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            LocationInputTextFieldGre(
                value = "Vị trí hiện tại",
                onValueChange = {},
                placeholder = "Điểm xuất phát...",
                isActive = false,
                accentColor = AppTheme.colors.routeStartColor,
                leadingIcon = Icons.Default.MyLocation,
                textColor = AppTheme.colors.routeStartTextColor,
            )
            LocationInputTextFieldGre(
                value = "",
                onValueChange = {},
                placeholder = "Bạn muốn đi đâu?",
                isActive = true,
                accentColor = AppTheme.colors.routeEndColor,
            )
        }
    }
}
