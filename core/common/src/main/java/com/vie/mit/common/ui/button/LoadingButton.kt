package com.vie.mit.common.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.vie.mit.common.theme.AppTheme

@Composable
fun LoadingButton(
    modifier: Modifier = Modifier,
    text: String = "",
    isLoading: Boolean = false,
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
    content: @Composable (BoxScope.() -> Unit) = {
        Text(
            text = text,
            style = AppTheme.typography.titleMedium.copy(color = AppTheme.colors.onPrimary)
        )
    },
    loading: @Composable (BoxScope.() -> Unit) = {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp), color = AppTheme.colors.onPrimary, strokeWidth = 2.dp
        )
    }
) {
    DebounceButton(
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.alpha(
                    if (isLoading) 0f else 1f
                )
            ) {
                content()
            }

            Box(
                modifier = Modifier.alpha(
                    if (isLoading) 1f else 0f
                )
            ) {
                loading()
            }
        }
    }
}