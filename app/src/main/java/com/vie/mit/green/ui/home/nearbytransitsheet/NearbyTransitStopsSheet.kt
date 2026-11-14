package com.vie.mit.green.ui.home.nearbytransitsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vie.mit.common.theme.AppTheme
import java.util.Locale

/**
 * NearbyTransitSheet: BottomSheet content for displaying public transit stops
 * within radius [selectedRadiusKm] around the current map center location.
 *
 * @param nearbyStops List of transit stops located within [selectedRadiusKm] km
 * @param selectedRadiusKm Radius parameter in kilometers (e.g. 0.5, 1.0, 2.0, 5.0)
 * @param onRadiusSelected Callback when user changes search radius parameter
 * @param onBack Callback to close sheet and return to HomeSheet
 * @param onStopClick Callback when user taps a stop item
 */
@Composable
fun NearbyTransitSheet(
    nearbyStops: List<NearbyStopItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    selectedRadiusKm: Double = 1.0,
    onRadiusSelected: (Double) -> Unit = {},
    onStopClick: (NearbyStopItem) -> Unit = {},
) {
    val availableRadii = listOf(0.5, 1.0, 2.0, 5.0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Header with title and close/back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Trạm dừng xung quanh",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(32.dp)
                    .background(AppTheme.colors.overlayColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Trở về Trang chủ",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Radius Filter Chips Row (x km parameter selection)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Bán kính:",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        labelColor = Color.White,
                        selectedContainerColor = AppTheme.colors.navyBlue,
                        selectedLabelColor = Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Info card showing location status and found count
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = AppTheme.colors.navyBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vị trí tâm bản đồ hiện tại",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tìm thấy ${nearbyStops.size} trạm dừng trong bán kính $selectedRadiusKm km quanh vị trí này.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. List of nearby stops or Empty State
        if (nearbyStops.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không tìm thấy trạm dừng nào trong bán kính $selectedRadiusKm km quanh khu vực này.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4B5563)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = nearbyStops,
                    key = { it.stopId }
                ) { stop ->
                    NearbyStopItemCard(
                        stop = stop,
                        onClick = { onStopClick(stop) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun NearbyStopItemCard(
    stop: NearbyStopItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, badgeColor) = when (stop.typeId.uppercase()) {
        "BUS" -> Icons.Default.DirectionsBus to Color(0xFF1E88E5)
        "METRO" -> Icons.Default.Subway to Color(0xFFE53935)
        "TRAIN" -> Icons.Default.Train to Color(0xFF8E24AA)
        "BIKE" -> Icons.AutoMirrored.Filled.DirectionsBike to Color(0xFF43A047)
        else -> Icons.Default.DirectionsTransit to AppTheme.colors.navyBlue
    }

    val formattedDistance = if (stop.distanceMeters < 1000) {
        "${stop.distanceMeters.toInt()}m"
    } else {
        String.format(Locale.getDefault(), "%.1f km", stop.distanceMeters / 1000f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stop.typeId,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
                if (stop.routesOrInfo.isNotBlank()) {
                    Text(
                        text = stop.routesOrInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4B5563)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formattedDistance,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.navyBlue
                )
            )
        }
    }
}
