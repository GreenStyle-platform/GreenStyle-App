package com.vie.mit.green.ui.home.common

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Standard Header for Nearby Transit Sheets (Bus, Subway, Bike, etc.).
 */
@Composable
fun NearbySheetHeader(
    modifier: Modifier = Modifier,
    title: String,
    icon: Int? = null,
    onBack: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        icon?.let {
            Image(
                painter = painterResource(icon), contentDescription = "transit icon"
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title)
        Spacer(modifier = Modifier.weight(1f))

        Image(
            modifier = Modifier.clickable(enabled = true, onClick = onBack),
            painter = painterResource(com.vie.mit.common.R.drawable.ic_close),
            contentDescription = "Close"
        )
    }
}

/**
 * Standard Radius Filter Chips row (0.5km, 1.0km, 2.0km, 5.0km).
 */
@Composable
fun RadiusFilterChipsRow(
    selectedRadiusKm: Double,
    onRadiusSelected: (Double) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    availableRadii: List<Double> = listOf(0.5, 1.0, 2.0, 5.0),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Bán kính:",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
            ),
        )
        availableRadii.forEach { radius ->
            val isSelected = selectedRadiusKm == radius
            FilterChip(
                selected = isSelected,
                onClick = { onRadiusSelected(radius) },
                label = {
                    Text(
                        text = "${radius} km",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    labelColor = Color.White,
                    selectedContainerColor = accentColor,
                    selectedLabelColor = Color.White,
                ),
                shape = RoundedCornerShape(12.dp),
            )
        }
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
    title: String = "Vị trí tâm bản đồ hiện tại",
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
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151),
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tìm thấy $itemCount $itemTypeName trong bán kính $selectedRadiusKm km",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                )
            }
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
