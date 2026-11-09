package com.vie.mit.mapbox.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vie.mit.mapbox.R

/**
 * Reusable Map Center Pin overlay composable.
 * Renders a fixed pin/dot at the center of the Map camera viewport.
 *
 * @param modifier Modifier for the overlay container.
 * @param showPin Whether the pin overlay is visible.
 * @param customContent Optional custom composable content for the pin. If null, renders [DefaultCenterPin].
 */
@Composable
fun BoxScope.MapCenterPin(
    modifier: Modifier = Modifier,
    showPin: Boolean = true,
    customContent: (@Composable () -> Unit)? = null,
) {
    if (showPin) {
        Box(
            modifier = modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center,
        ) {
            if (customContent != null) {
                customContent()
            } else {
                DefaultCenterPin()
            }
        }
    }
}

/**
 * Default center pin matching the LocationPuck style (#2BB32A green dot with white border & subtle shadow).
 */
@Composable
fun DefaultCenterPin(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    Image(
        painter = painterResource(id = R.drawable.ic_pin_map),
        modifier = modifier.size(size),
        contentDescription = "Pin Location"
    )
}
