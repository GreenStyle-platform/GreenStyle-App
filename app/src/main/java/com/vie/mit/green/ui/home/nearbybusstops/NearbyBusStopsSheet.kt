package com.vie.mit.green.ui.home.nearbybusstops

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.data.model.domain.map.StopArrival
import com.vie.mit.green.R
import com.vie.mit.green.ui.home.common.NearbyEmptyStateCard
import com.vie.mit.green.ui.home.common.NearbyLocationInfoCard
import com.vie.mit.green.ui.home.common.NearbySheetHeader
import com.vie.mit.green.ui.home.common.RadiusFilterChipsRow
import com.vie.mit.green.ui.home.common.formatDistance

// Arrival ETA status colors
private val EtaGreen = Color(0xFF2E7D32)
private val EtaOrange = Color(0xFFE65100)
private val EtaRed = Color(0xFFB71C1C)

/**
 * NearbyBusStopsSheet: BottomSheet content dedicated to nearby bus stops.
 *
 * Shows a list of bus stops near the current map center (sorted ascending by distance), each with:
 * - Stop name and walking distance
 * - Expandable list of routes with real-time/scheduled arrival times
 *
 * Visual theme uses [AppTheme.colors.busColor].
 *
 * @param uiState Combined state of bus stops + their arrivals
 * @param selectedRadiusKm Currently selected search radius in km
 * @param onRadiusSelected Callback when user selects a different radius
 * @param onBack Callback to close this sheet and return to HomeSheet
 * @param onRefresh Callback to reload arrivals data
 */
@Composable
fun NearbyBusStopsSheet(
    uiState: NearbyBusStopsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    selectedRadiusKm: Double = 1.0,
    onRadiusSelected: (Double) -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val busColor = AppTheme.colors.busColor

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // ── 1. Header ──────────────────────────────────────────────────────
        NearbySheetHeader(
            title = stringResource(R.string.nearby_bus_title),
            icon = com.vie.mit.mapbox.R.drawable.ic_transit_bus,
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ── 2. Radius Filter Chips ─────────────────────────────────────────
        RadiusFilterChipsRow(
            selectedRadiusKm = selectedRadiusKm,
            onRadiusSelected = onRadiusSelected,
            accentColor = busColor,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ── 3. Info Card ───────────────────────────────────────────────────
        NearbyLocationInfoCard(
            itemCount = uiState.busStopsWithArrivals.size,
            itemTypeName = "trạm xe buýt",
            selectedRadiusKm = selectedRadiusKm,
            accentColor = busColor,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── 4. Stop List or Empty State ────────────────────────────────────
        if (uiState.busStopsWithArrivals.isEmpty() && !uiState.isLoadingArrivals) {
            NearbyEmptyStateCard(
                icon = Icons.Default.DirectionsBus,
                message = "Không tìm thấy trạm xe buýt nào\ntrong bán kính $selectedRadiusKm km",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = uiState.busStopsWithArrivals,
                    key = { it.stop.stopId },
                ) { stopWithArrivals ->
                    BusStopCard(
                        stopWithArrivals = stopWithArrivals,
                        busColor = busColor,
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ── Bus Stop Item Card ─────────────────────────────────────────────────────────

@Composable
private fun BusStopCard(
    stopWithArrivals: NearbyBusStopWithArrivals,
    busColor: Color,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(stopWithArrivals.stop.stopId) { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "expand_arrow",
    )

    val formattedDistance = formatDistance(stopWithArrivals.stop.distanceMeters)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Stop Header Row ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Bus icon badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(busColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = "Xe buýt",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Stop name + subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stopWithArrivals.stop.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                        ),
                        maxLines = 2,
                    )
                    if (stopWithArrivals.stop.routesOrInfo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stopWithArrivals.stop.routesOrInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280),
                            maxLines = 1,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Distance + expand arrow
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedDistance,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = busColor,
                        ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Thu gọn" else "Xem tuyến xe",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle),
                    )
                }
            }

            // ── Arrivals Section (Expandable) ─────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                ),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = Color(0xFFE5E7EB),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    ArrivalsContent(
                        stopWithArrivals = stopWithArrivals,
                        busColor = busColor,
                    )
                }
            }
        }
    }
}

// ── Arrivals Content ───────────────────────────────────────────────────────────

@Composable
private fun ArrivalsContent(
    stopWithArrivals: NearbyBusStopWithArrivals,
    busColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            // Loading state
            stopWithArrivals.isLoadingArrivals -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = busColor,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Đang tải lịch trình...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                    )
                }
            }

            // Error state
            stopWithArrivals.arrivalsError != null -> {
                Text(
                    text = "Không thể tải lịch trình tuyến xe",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            }

            // Empty routes
            stopWithArrivals.routes.isEmpty() -> {
                Text(
                    text = "Không có tuyến xe nào đang hoạt động",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            }

            // Routes list
            else -> {
                Text(
                    text = "Các tuyến xe sẽ đến",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B7280),
                    ),
                )
                stopWithArrivals.routes.forEach { route ->
                    RouteArrivalRow(route = route, busColor = busColor)
                }
            }
        }
    }
}

// ── Individual Route Arrival Row ───────────────────────────────────────────────

@Composable
private fun RouteArrivalRow(
    route: StopArrival,
    busColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF9FAFB),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Route number badge
            Box(
                modifier = Modifier
                    .background(busColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = route.routeShortName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    ),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Route info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.headsign.ifBlank { route.routeLongName },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151),
                    ),
                    maxLines = 1,
                )
                if (route.routeLongName.isNotBlank() && route.headsign.isNotBlank()) {
                    Text(
                        text = route.routeLongName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF),
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Arrival times list
            if (route.arrivals.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE5E7EB),
                ) {
                    Text(
                        text = "Chưa có xe",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF6B7280),
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val displayArrivals = route.arrivals.take(3)
                    val isSingle = displayArrivals.size == 1
                    displayArrivals.forEach { arrival ->
                        val (etaText, etaColor) = when {
                            arrival.etaMinutes <= 1 -> "Sắp đến" to EtaRed
                            arrival.etaMinutes <= 5 -> (if (isSingle) "${arrival.etaMinutes} phút" else "${arrival.etaMinutes}p") to EtaOrange
                            arrival.etaMinutes <= 15 -> (if (isSingle) "${arrival.etaMinutes} phút" else "${arrival.etaMinutes}p") to EtaGreen
                            else -> (if (isSingle) "${arrival.etaMinutes} phút" else "${arrival.etaMinutes}p") to Color(0xFF6B7280)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = etaColor.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = etaText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = etaColor,
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
