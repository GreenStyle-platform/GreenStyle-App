package com.vie.mit.green.ui.home.nearbytransitsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Train
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
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.green.R
import com.vie.mit.green.ui.home.common.NearbyEmptyStateCard
import com.vie.mit.green.ui.home.common.NearbyLocationInfoCard
import com.vie.mit.green.ui.home.common.NearbySheetHeader
import com.vie.mit.green.ui.home.common.RadiusFilterChipsRow
import com.vie.mit.green.ui.home.common.formatDistance

/**
 * NearbyTransitSheet: BottomSheet content for displaying public transit stops
 * within radius [selectedRadiusKm] around the current map center location.
 *
 * @param nearbyStops List of transit stops located within [selectedRadiusKm] km (sorted by distance ascending)
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Header with title and close/back button
        NearbySheetHeader(
            title = stringResource(R.string.nearby_transit_stops),
            icon = null,
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Radius Filter Chips Row (x km parameter selection)
        RadiusFilterChipsRow(
            selectedRadiusKm = selectedRadiusKm,
            onRadiusSelected = onRadiusSelected,
            accentColor = AppTheme.colors.navyBlue,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Info card showing location status and found count
        NearbyLocationInfoCard(
            itemCount = nearbyStops.size,
            itemTypeName = "trạm dừng",
            selectedRadiusKm = selectedRadiusKm,
            accentColor = AppTheme.colors.navyBlue,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. List of nearby stops or Empty State
        if (nearbyStops.isEmpty()) {
            NearbyEmptyStateCard(
                icon = Icons.Default.DirectionsTransit,
                message = "Không tìm thấy trạm dừng nào trong bán kính $selectedRadiusKm km quanh khu vực này.",
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                ),
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
        TransitMode.BUS.name -> Icons.Default.DirectionsBus to AppTheme.colors.busColor
        TransitMode.SUBWAY.name -> Icons.Default.Subway to AppTheme.colors.metroColor
        TransitMode.RAIL.name -> Icons.Default.Train to Color(0xFF8E24AA)
        TravelMode.BICYCLE_RENT.name -> Icons.AutoMirrored.Filled.DirectionsBike to AppTheme.colors.bikeColor
        else -> Icons.Default.DirectionsTransit to AppTheme.colors.navyBlue
    }

    val formattedDistance = formatDistance(stop.distanceMeters)

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
                    color = badgeColor
                )
            )
        }
    }
}
