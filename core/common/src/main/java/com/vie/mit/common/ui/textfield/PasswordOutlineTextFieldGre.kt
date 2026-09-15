package com.vie.mit.common.ui.textfield

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vie.mit.common.theme.GreenTheme

/**
 * A reusable Password OutlinedTextField for the app with a title label on top and a visibility toggle.
 *
 * @param value The input text to be shown in the text field.
 * @param onValueChange The callback that is triggered when the input service updates the text.
 * @param title The title text to be displayed above the text field.
 * @param modifier The [Modifier] to be applied to this text field container.
 * @param placeholder The optional placeholder to be displayed when the text field is empty.
 * @param isError Indicates if the text field's current value is in error.
 * @param errorMessage The error message to be displayed below the text field when [isError] is true.
 * @param keyboardOptions Software keyboard options. Defaults to [KeyboardType.Password].
 * @param keyboardActions When the software keyboard emits an IME action, the corresponding callback is called.
 * @param imeAction The IME action for the keyboard.
 */
@Composable
fun PasswordOutlineTextFieldGre(
    modifier: Modifier = Modifier,
    value: String = "",
    onValueChange: (String) -> Unit,
    title: String = "",
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    imeAction: ImeAction = ImeAction.Done,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlineTextFieldGre(
        value = value,
        onValueChange = onValueChange,
        title = title,
        modifier = modifier,
        placeholder = placeholder,
        isError = isError,
        errorMessage = errorMessage,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = keyboardOptions.copy(imeAction = imeAction),
        keyboardActions = keyboardActions,
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility
            else Icons.Filled.VisibilityOff
            val description = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"

            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, contentDescription = description)
            }
        },
        singleLine = true
    )
}

@Preview(showBackground = true)
@Composable
private fun PasswordOutlineTextFieldGrePreview() {
    GreenTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PasswordOutlineTextFieldGre(
                value = "",
                onValueChange = {},
                title = "Mật khẩu",
                placeholder = "Nhập mật khẩu của bạn"
            )
            Spacer(modifier = Modifier.height(16.dp))
            PasswordOutlineTextFieldGre(
                value = "123456",
                onValueChange = {},
                title = "Xác nhận mật khẩu",
                placeholder = "Nhập lại mật khẩu",
                isError = true,
                errorMessage = "Mật khẩu không khớp"
            )
        }
    }
}
