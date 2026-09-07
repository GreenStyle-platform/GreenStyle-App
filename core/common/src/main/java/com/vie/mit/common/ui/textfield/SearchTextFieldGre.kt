package com.vie.mit.common.ui.textfield

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vie.mit.common.ui.theme.AppTheme
import com.vie.mit.common.ui.theme.GreenTheme

/**
 * A search text field composable for the GreenApp.
 *
 * @param value The input text to be shown in the text field.
 * @param onValueChange The callback that is triggered when the input service updates the text.
 * @param modifier The [Modifier] to be applied to this text field.
 * @param placeholder The optional placeholder to be displayed when the text field is empty.
 * @param onSearch The callback that is triggered when the search action is performed.
 */
@Composable
fun SearchTextFieldGre(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    onSearch: (String) -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        placeholder = {
            Text(
                text = placeholder,
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.secondaryText
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = AppTheme.colors.secondaryText,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = AppTheme.colors.secondaryText,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            onValueChange("")
                        }
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = AppTheme.colors.surfaceDim,
            unfocusedContainerColor = AppTheme.colors.surfaceDim,
            disabledContainerColor = AppTheme.colors.surfaceDim,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = AppTheme.colors.primary,
            focusedTextColor = AppTheme.colors.primaryText,
            unfocusedTextColor = AppTheme.colors.primaryText
        ),
        textStyle = AppTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch(value)
                keyboardController?.hide()
            }
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchTextFieldGrePreview() {
    GreenTheme {
        SearchTextFieldGre(
            value = "",
            onValueChange = {},
            placeholder = "Tìm kiếm điểm đến...",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchTextFieldGreWithValuePreview() {
    GreenTheme {
        SearchTextFieldGre(
            value = "Vinhome Ocean Park",
            onValueChange = {},
            placeholder = "Tìm kiếm...",
            modifier = Modifier.padding(16.dp)
        )
    }
}
