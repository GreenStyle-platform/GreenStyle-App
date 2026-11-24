package com.vie.mit.green.ui.home.nearbysubwaystations

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.data.model.domain.map.BusRoute
import com.vie.mit.green.R
import com.vie.mit.green.ui.home.common.NearbyEmptyStateCard
import com.vie.mit.green.ui.home.common.NearbyLocationInfoCard
import com.vie.mit.green.ui.home.common.NearbySheetHeader
import com.vie.mit.green.ui.home.common.formatDistance
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyStopItem

/**
 * NearbySubwayStationsSheet: BottomSheet content dedicated to nearby subway / metro stations and routes.
 *
 * Mode 1 (Stations): Displays subway stations sorted by distance (ascending) around current map center.
 * Mode 2 (Routes): Displays subway routes fetched from the backend.
 *
 * Colors and visual theme are driven by [AppTheme.colors.metroColor].
 */
@Composable
fun NearbySubwayStationsSheet(
    nearbyStops: List<NearbyStopItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: NearbySubwayStationUiState = NearbySubwayStationUiState(),
    selectedRadiusKm: Double = 1.0,
    onRadiusSelected: (Double) -> Unit = {},
    onTabSelected: (SubwaySheetTab) -> Unit = {},
    onRetryLoadRoutes: () -> Unit = {},
    onRouteClick: (String) -> Unit = {},
    onStopClick: (String) -> Unit = {},
) {
    val metroColor = AppTheme.colors.metroColor

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Header
        NearbySheetHeader(
            title = stringResource(R.string.nearby_subway_title),
            icon = com.vie.mit.mapbox.R.drawable.ic_transit_metro,
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 1.5 Mode Switcher Buttons (Stops / Metro Routes)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val isStationsSelected = uiState.selectedTab == SubwaySheetTab.STATIONS
            val isRoutesSelected = uiState.selectedTab == SubwaySheetTab.ROUTES

            // Left tab: "Trạm dừng"
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape = RoundedCornerShape(8.dp))
                    .background(
                        if (isStationsSelected) AppTheme.colors.blackOverlay20 else AppTheme.colors.blackOverlay10
                    )
                    .clickable(
                        enabled = !isStationsSelected,
                        onClick = { onTabSelected(SubwaySheetTab.STATIONS) }
                    )
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Trạm dừng",
                    style = AppTheme.typography.bodyMedium.copy(
                        fontWeight = if (isStationsSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isStationsSelected) Color.White else Color.White.copy(alpha = 0.8f),
                    ),
                )
            }

            // Right tab: "Các tuyến metro"
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isRoutesSelected) AppTheme.colors.blackOverlay20 else AppTheme.colors.blackOverlay10
                    )
                    .clickable(
                        enabled = !isRoutesSelected,
                        onClick = { onTabSelected(SubwaySheetTab.ROUTES) }
                    )
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Subway,
                    contentDescription = null,
                    tint = if (isRoutesSelected) Color.White else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Các tuyến metro",
                    style = AppTheme.typography.bodyMedium.copy(
                        fontWeight = if (isRoutesSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isRoutesSelected) Color.White else Color.White.copy(alpha = 0.8f),
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (uiState.selectedTab) {
            SubwaySheetTab.STATIONS -> {
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
                    val formattedRadius = formatDistance((selectedRadiusKm * 1000).toFloat())
                    NearbyEmptyStateCard(
                        icon = Icons.Default.Subway,
                        message = "Không tìm thấy ga tàu điện ngầm nào\ntrong bán kính $formattedRadius",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = nearbyStops,
                            key = { it.stopId },
                        ) { stop ->
                            SubwayStationCard(
                                stop = stop,
                                metroColor = metroColor,
                                onClick = { onStopClick(stop.stopId) },
                            )
                        }
                    }
                }
            }

            SubwaySheetTab.ROUTES -> {
                when {
                    uiState.isLoadingRoutes -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = metroColor)
                        }
                    }

                    uiState.routesErrorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = uiState.routesErrorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFEF4444),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onRetryLoadRoutes,
                                    colors = ButtonDefaults.buttonColors(containerColor = metroColor),
                                ) {
                                    Text("Thử lại", color = Color.White)
                                }
                            }
                        }
                    }

                    uiState.subwayRoutes.isEmpty() -> {
                        NearbyEmptyStateCard(
                            icon = Icons.Default.Subway,
                            message = "Không tìm thấy tuyến metro nào",
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
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = uiState.subwayRoutes,
                                key = { it.routeId },
                            ) { route ->
                                SubwayRouteCard(
                                    route = route,
                                    metroColor = metroColor,
                                    onClick = { onRouteClick(route.routeId) },
                                )
                            }
                        }
                    }
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
            // Metro Icon Badge using ic_metro_station.png

                Image(
                    painter = painterResource(id = R.drawable.ic_metro_station),
                    contentDescription = "Ga tàu điện ngầm",
                    modifier = Modifier.size(32.dp),
                )

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

@Composable
private fun SubwayRouteCard(
    route: BusRoute,
    metroColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val badgeColor = route.routeColor?.let {
                try {
                    Color(android.graphics.Color.parseColor(if (it.startsWith("#")) it else "#$it"))
                } catch (e: Exception) {
                    metroColor
                }
            } ?: metroColor

            Box(
                modifier = Modifier
                    .background(badgeColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = route.routeShortName.ifBlank { "Metro" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.routeLongName.ifBlank { route.routeShortName },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                    ),
                    maxLines = 2,
                )
                if (route.routeDesc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = route.routeDesc,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
