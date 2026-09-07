package com.vie.mit.common.ui.text

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.vie.mit.common.ui.button.debounceClick

@Composable
fun TextClickable(
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    text: String,
    indication: Indication? = null,
    onClick: () -> Unit,
    debounceTime: Long = 370L,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = indication,
            role = Role.Button,
            onClick = debounceClick(debounceTime = debounceTime, onClick = onClick)
        ),
        text = text,
        textAlign = textAlign,
        style = style,
        color = color
    )
}
