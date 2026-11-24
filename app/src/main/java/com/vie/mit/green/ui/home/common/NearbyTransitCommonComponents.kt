package com.vie.mit.green.ui.home.common

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vie.mit.common.theme.AppTheme
import java.util.Locale

/**
 * Converts any Android Drawable resource ID (including VectorDrawable, rasterized asset,
 * LayerDrawable, shape, etc.) into a Compose [Painter] safely.
 */
@Composable
fun rememberDrawablePainter(@DrawableRes id: Int?): Painter? {
    if (id == null || id == 0) return null
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(id, context, density) {
        try {
            val drawable = ContextCompat.getDrawable(context, id) ?: return@remember null
            val width =
                if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else (24 * density.density).toInt()
            val height =
                if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else (24 * density.density).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            BitmapPainter(bitmap.asImageBitmap())
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Standard Header for Nearby Transit Sheets (Bus, Subway, Bike, etc.).
 */
@Composable
@Preview
fun NearbySheetHeaderPreview(
) {
    NearbySheetHeader(
        modifier = Modifier.fillMaxWidth(), title = "Bus", icon = null, onBack = {})
}

@Composable
fun NearbySheetHeader(
    modifier: Modifier = Modifier,
    title: String,
    icon: Int? = null,
    onRefresh: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        val iconPainter: Painter? = rememberDrawablePainter(icon)
        if (iconPainter != null) {
            Image(
                painter = iconPainter,
                contentDescription = "transit icon",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = title, style = AppTheme.typography.headlineMedium.copy(
                color = AppTheme.colors.white, fontSize = 20.sp
            )
        )
        Spacer(modifier = Modifier.weight(1f))

        if (onRefresh != null) {
            IconButton(
                onClick = onRefresh, modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = AppTheme.colors.white,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Image(
            modifier = Modifier
                .size(32.dp)
                .padding(8.dp)
                .clickable(enabled = true, onClick = onBack),
            painter = painterResource(com.vie.mit.common.R.drawable.ic_close),
            contentDescription = "Close"
        )
    }
}

/**
 * Standard Information Card displaying current map center status and found stop count.
 */
@Composable
fun NearbyLocationInfoCard(
    itemCount: Int,
    itemTypeName: String,
    selectedRadiusKm: Double,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            val formattedRadius = formatDistance((selectedRadiusKm * 1000).toFloat())
            Text(
                text = "Tìm thấy $itemCount $itemTypeName trong bán kính $formattedRadius",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.secondaryText,
            )
        }
    }
}

/**
 * Standard Empty State Card when no transit stops are found in the radius.
 */
@Composable
fun NearbyEmptyStateCard(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFD1D5DB),
                    modifier = Modifier.size(40.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280),
                )
            }
        }
    }
}

/**
 * Standard distance formatting helper (e.g. "350 m" or "1.2 km").
 */
fun formatDistance(meters: Float): String = if (meters < 1000) {
    "${meters.toInt()} m"
} else {
    String.format(Locale.getDefault(), "%.1f km", meters / 1000f)
}
