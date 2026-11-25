package com.vie.mit.mapbox.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vie.mit.common.theme.AppTheme
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Default fixed radius in DP for the nearby search circle on screen.
 * Diameter = 260.dp.
 */
val DEFAULT_NEARBY_RADIUS_CIRCLE_DP: Dp = 130.dp

/**
 * Formats radius in meters to human-readable distance (e.g. "450 m" or "1.2 km").
 */
fun formatRadiusDistance(radiusKm: Double): String {
    val meters = (radiusKm * 1000.0).roundToInt()
    return if (meters < 1000) {
        "$meters m"
    } else {
        String.format(Locale.getDefault(), "%.1f km", radiusKm)
    }
}

/**
 * Reusable Overlay Composable displaying a fixed transparent circle representing
 * the nearby search radius on top of the Map view, with an attached distance badge.
 *
 * "Không có overlay": Transparent inside and outside (no dimming/tinted scrim).
 *
 * @param modifier Modifier for the container.
 * @param isVisible Whether the circle overlay is visible.
 * @param radiusDp Radius of the circle in DP (default [DEFAULT_NEARBY_RADIUS_CIRCLE_DP]).
 * @param radiusKm Current dynamically calculated radius in KM to display on the badge.
 * @param strokeColor Color of the circle outline.
 * @param cameraBottomPadding Vertical offset padding (e.g. from bottom sheet) to keep circle centered.
 */
@Composable
fun BoxScope.NearbyRadiusCircleOverlay(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    radiusDp: Dp = DEFAULT_NEARBY_RADIUS_CIRCLE_DP,
    radiusKm: Double = 1.0,
    strokeColor: Color = AppTheme.colors.navyBlue,
    cameraBottomPadding: Float = 0f,
) {
    if (!isVisible) return

    val diameter = radiusDp * 2

    Box(
        modifier = modifier
            .align(Alignment.Center)
            .offset {
                IntOffset(0, -((cameraBottomPadding / 2f).roundToInt()))
            }
            .size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        // 1. Fixed circle boundary outline (transparent interior - không có overlay)
        Box(
            modifier = Modifier
                .size(diameter)
                .border(
                    width = 2.dp, color = strokeColor.copy(alpha = 0.85f), shape = CircleShape
                )
        )

        // 2. Radius badge pill attached at the top of the circle
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(strokeColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bán kính: ${formatRadiusDistance(radiusKm)}",
                    style = AppTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = AppTheme.colors.primaryText
                    )
                )
            }
        }
    }
}
