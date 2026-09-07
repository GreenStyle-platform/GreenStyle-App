package com.vie.mit.common.ui.textfield

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vie.mit.common.ui.theme.AppTheme
import com.vie.mit.common.ui.theme.GreenTheme

/**
 * A reusable OutlinedTextField for the app with a title label on top.
 *
 * @param value The input text to be shown in the text field.
 * @param onValueChange The callback that is triggered when the input service updates the text.
 * @param title The title text to be displayed above the text field.
 * @param modifier The [Modifier] to be applied to this text field container.
 * @param label The optional label to be displayed inside the text field container.
 * @param placeholder The optional placeholder to be displayed when the text field is empty.
 * @param leadingIcon The optional leading icon to be displayed at the beginning of the text field container.
 * @param trailingIcon The optional trailing icon to be displayed at the end of the text field container.
 * @param isError Indicates if the text field's current value is in error.
 * @param errorMessage The error message to be displayed below the text field when [isError] is true.
 * @param enabled Controls the enabled state of the text field.
 * @param visualTransformation The transformation to be applied to the visual representation of the text field's value.
 * @param keyboardOptions Software keyboard options.
 * @param keyboardActions When the software keyboard emits an IME action, the corresponding callback is called.
 * @param singleLine When set to true, this text field becomes a single horizontally scrolling text field.
 * @param maxLines The maximum height in terms of maximum number of visible lines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineTextFieldGre(
    value: String = "",
    onValueChange: (String) -> Unit,
    title: String = "",
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
) {
    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.primaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            isError = isError,
            label = label?.let {
                { Text(text = it) }
            },
            placeholder = placeholder?.let {
                { Text(text = it) }
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.primary,
                unfocusedBorderColor = AppTheme.colors.outline,
                errorBorderColor = AppTheme.colors.error,
                focusedLabelColor = AppTheme.colors.primary,
                unfocusedLabelColor = AppTheme.colors.secondaryText,
                errorLabelColor = AppTheme.colors.error,
                focusedTextColor = AppTheme.colors.onSurface,
                unfocusedTextColor = AppTheme.colors.onSurface,
                focusedPlaceholderColor = AppTheme.colors.secondaryText,
                unfocusedPlaceholderColor = AppTheme.colors.secondaryText,
                cursorColor = AppTheme.colors.primary,
                selectionColors = TextSelectionColors(
                    handleColor = AppTheme.colors.primary,
                    backgroundColor = AppTheme.colors.primary.copy(alpha = 0.4f)
                )
            )
        )

        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = AppTheme.colors.error,
                style = AppTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OutlineTextFieldGrePreview() {
    GreenTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlineTextFieldGre(
                value = "",
                onValueChange = {},
                title = "Họ và tên",
                placeholder = "Nhập họ và tên của bạn"
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlineTextFieldGre(
                value = "Nguyễn Văn A",
                onValueChange = {},
                title = "Số điện thoại",
                placeholder = "Nhập số điện thoại",
                isError = true,
                errorMessage = "Số điện thoại không hợp lệ"
            )
        }
    }
}
