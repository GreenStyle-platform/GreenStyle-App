package com.vie.mit.green.ui.home.nearbytransitsheet

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsTransit
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.green.R
import com.vie.mit.green.ui.home.common.NearbyEmptyStateCard
import com.vie.mit.green.ui.home.common.NearbyLoadingStateCard
import com.vie.mit.green.ui.home.common.NearbyLocationInfoCard
import com.vie.mit.green.ui.home.common.NearbySheetHeader
import com.vie.mit.green.ui.home.common.formatDistance

/**
 * NearbyTransitSheet: BottomSheet content for displaying public transit stops
 * within radius [selectedRadiusKm] around the current map center location.
 *
 * @param nearbyStops List of transit stops located within [selectedRadiusKm] km (sorted by distance ascending)
 * @param isLoadingStops Whether transit stops for the current viewport are still loading
 * @param selectedRadiusKm Radius parameter in kilometers (e.g. 0.5, 1.0, 2.0, 5.0)
 * @param onBack Callback to close sheet and return to HomeSheet
 * @param onStopClick Callback when user taps a stop item
 */
@Composable
fun NearbyTransitSheet(
    nearbyStops: List<NearbyStopItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isLoadingStops: Boolean = false,
    selectedRadiusKm: Double = 1.0,
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

        // 3. Info card showing location status and found count
        NearbyLocationInfoCard(
            itemCount = nearbyStops.size,
            itemTypeName = "trạm dừng",
            selectedRadiusKm = selectedRadiusKm,
            accentColor = AppTheme.colors.navyBlue,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. List of nearby stops, Loading State, or Empty State
        when {
            nearbyStops.isEmpty() && isLoadingStops -> {
                NearbyLoadingStateCard(
                    accentColor = AppTheme.colors.navyBlue,
                    message = "Đang tìm trạm dừng gần đây...",
                )
            }

            nearbyStops.isEmpty() -> {
                val formattedRadius = formatDistance((selectedRadiusKm * 1000).toFloat())
                NearbyEmptyStateCard(
                    icon = Icons.Default.DirectionsTransit,
                    message = "Không tìm thấy trạm dừng nào trong bán kính $formattedRadius quanh khu vực này.",
                )
            }

            else -> {
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
                        key = { "${it.typeId}_${it.stopId}" }
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
}

@Composable
private fun NearbyStopItemCard(
    stop: NearbyStopItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeColor = when (stop.typeId.uppercase()) {
        TransitMode.BUS.name -> AppTheme.colors.busColor
        TransitMode.SUBWAY.name, "METRO" -> AppTheme.colors.metroColor
        TransitMode.RAIL.name, "TRAIN" -> AppTheme.colors.railColor
        TravelMode.BICYCLE_RENT.name, TravelMode.BICYCLE.name, "BIKE" -> AppTheme.colors.bikeColor
        else -> AppTheme.colors.navyBlue
    }

    val formattedDistance = formatDistance(stop.distanceMeters)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NearbyStopBadgeIcon(
                typeId = stop.typeId,
                badgeColor = badgeColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedDistance,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
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

@Composable
private fun NearbyStopBadgeIcon(
    typeId: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
) {
    when (typeId.uppercase()) {
        TransitMode.BUS.name -> {
            Image(
                painter = painterResource(id = R.drawable.ic_bus_stop),
                contentDescription = "Trạm xe buýt",
                modifier = modifier.size(36.dp)
            )
        }
        TransitMode.SUBWAY.name, "METRO" -> {
            Image(
                painter = painterResource(id = R.drawable.ic_metro_station),
                contentDescription = "Ga tàu điện ngầm",
                modifier = modifier.size(36.dp)
            )
        }
        TravelMode.BICYCLE_RENT.name, TravelMode.BICYCLE.name, "BIKE" -> {
            Box(
                modifier = modifier
                    .size(36.dp)
                    .background(badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                    contentDescription = "Trạm xe đạp",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        TransitMode.RAIL.name, "TRAIN" -> {
            Box(
                modifier = modifier
                    .size(36.dp)
                    .background(badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Train,
                    contentDescription = "Trạm tàu hỏa",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        else -> {
            Box(
                modifier = modifier
                    .size(36.dp)
                    .background(badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsTransit,
                    contentDescription = typeId,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

