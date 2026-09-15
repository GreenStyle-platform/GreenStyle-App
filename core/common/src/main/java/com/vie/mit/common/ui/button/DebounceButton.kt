package com.vie.mit.common.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.vie.mit.common.theme.AppTheme
import kotlin.math.abs

@Composable
fun DebounceButton(
    modifier: Modifier = Modifier,
    debounceTime: Long = 500L,
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = AppTheme.colors.primaryButtonBackground,
        contentColor = AppTheme.colors.onPrimary
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (RowScope.() -> Unit)
) {
    Button(
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        onClick = debounceClick(debounceTime = debounceTime) {
            onClick()
        }) {
        content()
    }
}

@Composable
fun DebounceIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    debounceTime: Long = 700L,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    IconButton(
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content,
        onClick = debounceClick(debounceTime = debounceTime) {
            onClick()
        })

}

@Composable
fun debounceClick(
    debounceTime: Long = 500L, onClick: () -> Unit
): () -> Unit {
    val currentOnClick by rememberUpdatedState(onClick)
    var lastClickTime by remember {
        mutableLongStateOf(0L)
    }
    return remember(debounceTime) {
        {
            val currentTime = System.currentTimeMillis()
            if (abs(currentTime - lastClickTime) > debounceTime) {
                lastClickTime = currentTime
                currentOnClick()
            }
        }
    }
}