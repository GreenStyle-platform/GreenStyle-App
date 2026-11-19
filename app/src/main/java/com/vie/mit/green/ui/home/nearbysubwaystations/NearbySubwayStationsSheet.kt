package com.vie.mit.green.ui.home.nearbysubwaystations

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.green.R
import com.vie.mit.green.ui.home.common.NearbyEmptyStateCard
import com.vie.mit.green.ui.home.common.NearbyLocationInfoCard
import com.vie.mit.green.ui.home.common.NearbySheetHeader
import com.vie.mit.green.ui.home.common.RadiusFilterChipsRow
import com.vie.mit.green.ui.home.common.formatDistance
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyStopItem

/**
 * NearbySubwayStationsSheet: BottomSheet content dedicated to nearby subway / metro stations.
 *
 * Displays subway stations sorted by distance (ascending) around the current map center.
 * Colors and visual theme are driven by [AppTheme.colors.metroColor].
 */
@Composable
fun NearbySubwayStationsSheet(
    nearbyStops: List<NearbyStopItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    selectedRadiusKm: Double = 1.0,
    onRadiusSelected: (Double) -> Unit = {},
    onStopClick: (NearbyStopItem) -> Unit = {},
) {
    val metroColor = AppTheme.colors.metroColor

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Header
        NearbySheetHeader(
            title = stringResource(R.string.nearby_bus_title),
            icon = com.vie.mit.mapbox.R.drawable.ic_transit_metro,
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Radius Filter Chips
        RadiusFilterChipsRow(
            selectedRadiusKm = selectedRadiusKm,
            onRadiusSelected = onRadiusSelected,
            accentColor = metroColor,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Info Card
        NearbyLocationInfoCard(
            itemCount = nearbyStops.size,
            itemTypeName = "ga tàu điện ngầm",
            selectedRadiusKm = selectedRadiusKm,
            accentColor = metroColor,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Stations List or Empty State
        if (nearbyStops.isEmpty()) {
            NearbyEmptyStateCard(
                icon = Icons.Default.Subway,
                message = "Không tìm thấy ga tàu điện ngầm nào\ntrong bán kính $selectedRadiusKm km",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = nearbyStops,
                    key = { it.stopId },
                ) { stop ->
                    SubwayStationCard(
                        stop = stop,
                        metroColor = metroColor,
                        onClick = { onStopClick(stop) },
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SubwayStationCard(
    stop: NearbyStopItem,
    metroColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDistance = formatDistance(stop.distanceMeters)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Metro Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(metroColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Subway,
                    contentDescription = "Ga tàu điện ngầm",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Station Name & Line info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                    ),
                    maxLines = 2,
                )
                if (stop.routesOrInfo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stop.routesOrInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Distance & Forward indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedDistance,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = metroColor,
                    ),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
