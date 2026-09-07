package com.vie.mit.common.extension

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A modifier that clears focus when the user taps on the component it's applied to.
 * Useful for root layouts to hide the keyboard when tapping outside of text fields.
 */
fun Modifier.addFocusCleaner(focusManager: FocusManager): Modifier {
    return this.pointerInput(Unit) {
        detectTapGestures(onTap = {
            focusManager.clearFocus()
        })
    }
}
