package com.vie.mit.common.ui.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vie.mit.common.R

/**
 * A floating back button with customizable size, elevation, color, and icon.
 *
 * @param onClick Callback triggered when the button is pressed.
 * @param modifier Custom modifier for positioning and padding (e.g. align or padding).
 * @param size Overall diameter of the floating button.
 * @param iconSize Size of the inner back icon.
 * @param containerColor Background color of the surface.
 * @param shadowElevation Elevation of the shadow underneath.
 * @param shape Shape of the button, defaulting to [CircleShape].
 * @param painter Painter for the back icon image, defaulting to [R.drawable.ic_back].
 * @param contentDescription Content description for accessibility.
 */
@Composable
fun FloatingBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 16.dp,
    containerColor: Color = Color.White,
    shadowElevation: Dp = 6.dp,
    shape: Shape = CircleShape,
    painter: Painter = painterResource(R.drawable.ic_back),
    contentDescription: String? = "Back",
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        shadowElevation = shadowElevation,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
