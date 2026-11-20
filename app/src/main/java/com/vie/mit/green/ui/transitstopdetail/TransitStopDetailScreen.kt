package com.vie.mit.green.ui.transitstopdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vie.mit.common.screenbottomsheet.BottomSheetConfig
import com.vie.mit.common.screenbottomsheet.CommonScreenBottomSheet
import com.vie.mit.common.screenbottomsheet.CustomBottomSheetValue
import com.vie.mit.common.screenbottomsheet.rememberCustomBottomSheetState
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.data.model.domain.map.StopArrival
import com.vie.mit.data.model.domain.map.VehicleArrival
import com.vie.mit.mapbox.composable.TransitStopFocusMap
import com.vie.mit.mapbox.model.TransitTypeConfig

/**
 * Screen displaying the details of a transit stop/station.
 *
 * Uses [CommonScreenBottomSheet] with:
 * - [TransitStopFocusMap] in the background focused on the stop coordinate.
 * - BottomSheet containing stop info and routes passing through with arrival schedules.
 */
@Composable
fun TransitStopDetailScreen(
    viewModel: TransitStopDetailViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSheetState = rememberCustomBottomSheetState(
        initialValue = CustomBottomSheetValue.PartiallyExpanded
    )

    val sheetConfig = remember {
        BottomSheetConfig(
            peekedRatio = 0.20f,
            partiallyExpandedRatio = 0.50f,
            expandedRatio = 0.90f,
        )
    }

    // Camera bottom padding syncing with BottomSheet
    val cameraBottomPadding by remember(bottomSheetState) {
        derivedStateOf {
            if (bottomSheetState.currentValue == CustomBottomSheetValue.Hidden) {
                0f
            } else {
                val partialHeight = bottomSheetState.partiallyExpandedHeightPx
                if (partialHeight > 0f) {
                    bottomSheetState.sheetHeightPx.coerceAtMost(partialHeight)
                } else {
                    bottomSheetState.sheetHeightPx
                }
            }
        }
    }

    val transitTypeConfig = remember(uiState.typeId) {
        when (uiState.typeId.uppercase()) {
            "SUBWAY", "METRO" -> TransitTypeConfig(
                typeId = "SUBWAY",
                iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_metro,
                clusterColor = 0xFFE53935.toInt(),
                iconSize = 1.2,
            )
            "BICYCLE", "BICYCLE_RENT", "BIKE" -> TransitTypeConfig(
                typeId = "BICYCLE_RENT",
                iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bike,
                clusterColor = 0xFF43A047.toInt(),
                iconSize = 1.2,
            )
            "RAIL", "TRAIN" -> TransitTypeConfig(
                typeId = "RAIL",
                iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_train,
                clusterColor = 0xFF8E24AA.toInt(),
                iconSize = 1.2,
            )
            else -> TransitTypeConfig(
                typeId = "BUS",
                iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bus,
                clusterColor = 0xFF1E88E5.toInt(),
                iconSize = 1.2,
            )
        }
    }

    CommonScreenBottomSheet(
        modifier = modifier.fillMaxSize(),
        initState = bottomSheetState,
        config = sheetConfig,
        backgroundContent = { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Background focused map
                TransitStopFocusMap(
                    stopLatitude = uiState.latitude,
                    stopLongitude = uiState.longitude,
                    stopName = uiState.stopName,
                    transitTypeConfig = transitTypeConfig,
                    cameraBottomPadding = cameraBottomPadding,
                    showUserLocation = true,
                    zoom = 16.5,
                )

                // Top-Left Floating Back Button
                Surface(
                    onClick = viewModel::navigateBack,
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 48.dp, start = 16.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = AppTheme.colors.black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        sheetContent = {
            TransitStopDetailSheetContent(
                uiState = uiState,
                onBack = viewModel::navigateBack,
                onRetry = viewModel::loadStopDetail,
            )
        }
    )
}

@Composable
private fun TransitStopDetailSheetContent(
    uiState: TransitStopDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 4.dp)
                .size(width = 36.dp, height = 4.dp)
                .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
        )

        // Header: Back icon + Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.stopName.ifEmpty { "Chi tiết trạm dừng" },
                    style = AppTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = getTransitIcon(uiState.typeId),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mã trạm: ${uiState.stopId}",
                        style = AppTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.85f)
                        ),
                    )
                }
            }
        }

        // Optional address
        if (!uiState.address.isNullOrBlank()) {
            Text(
                text = uiState.address,
                style = AppTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.75f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp),
            color = Color.White.copy(alpha = 0.2f),
        )

        // Body content
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Đang tải thông tin tuyến và lịch trình...",
                            style = AppTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.85f)
                            ),
                        )
                    }
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = uiState.error,
                            style = AppTheme.typography.bodyMedium.copy(color = Color.White),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = AppTheme.colors.primary,
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Thử lại")
                        }
                    }
                }
            }

            uiState.routeArrivals.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Không có thông tin tuyến xe qua trạm này",
                        style = AppTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.75f)
                        ),
                    )
                }
            }

            else -> {
                Text(
                    text = "Các tuyến đi qua trạm (${uiState.routeArrivals.size})",
                    style = AppTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = uiState.routeArrivals,
                        key = { it.routeId },
                    ) { routeArrival ->
                        RouteArrivalCard(routeArrival = routeArrival)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteArrivalCard(
    routeArrival: StopArrival,
    modifier: Modifier = Modifier,
) {
    val routeColor = remember(routeArrival.routeColor) {
        parseHexColor(routeArrival.routeColor, defaultColor = Color(0xFF1E88E5))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Route Header: Badge + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Route number badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = routeColor,
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Text(
                        text = routeArrival.routeShortName.ifEmpty { routeArrival.routeId },
                        style = AppTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                // Route long name / destination
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routeArrival.routeLongName.ifEmpty { "Tuyến ${routeArrival.routeShortName}" },
                        style = AppTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.primaryText,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (routeArrival.headsign.isNotBlank()) {
                        Text(
                            text = "Hướng đi: ${routeArrival.headsign}",
                            style = AppTheme.typography.bodySmall.copy(
                                color = AppTheme.colors.secondaryText
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Arrivals Schedule Section
            if (routeArrival.arrivals.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = AppTheme.colors.secondaryText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lịch trình đến:",
                        style = AppTheme.typography.bodySmall.copy(
                            color = AppTheme.colors.secondaryText,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(routeArrival.arrivals) { arrival ->
                            ArrivalChip(arrival = arrival, accentColor = routeColor)
                        }
                    }
                }
            } else {
                Text(
                    text = "Chưa có giờ xuất bến sắp tới",
                    style = AppTheme.typography.bodySmall.copy(
                        color = AppTheme.colors.disabledColor
                    ),
                )
            }
        }
    }
}

@Composable
private fun ArrivalChip(
    arrival: VehicleArrival,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val isSoon = arrival.etaMinutes in 0..5
    val chipBg = if (isSoon) accentColor.copy(alpha = 0.15f) else Color(0xFFF3F4F6)
    val textColor = if (isSoon) accentColor else Color(0xFF374151)

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = chipBg,
        modifier = modifier
    ) {
        Text(
            text = when {
                arrival.etaMinutes <= 0 -> "Đang đến"
                arrival.etaMinutes < 60 -> "${arrival.etaMinutes}p"
                !arrival.arrivalTime.isNullOrBlank() -> arrival.arrivalTime.orEmpty()
                else -> "${arrival.etaMinutes}p"
            },
            style = AppTheme.typography.bodySmall.copy(
                fontWeight = if (isSoon) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                fontSize = 11.sp,
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun getTransitIcon(typeId: String): ImageVector {
    return when (typeId.uppercase()) {
        "SUBWAY", "METRO" -> Icons.Default.DirectionsSubway
        "BICYCLE", "BICYCLE_RENT", "BIKE" -> Icons.Default.PedalBike
        else -> Icons.Default.DirectionsBus
    }
}

private fun parseHexColor(hexString: String?, defaultColor: Color): Color {
    if (hexString.isNullOrBlank()) return defaultColor
    return try {
        val cleanHex = hexString.removePrefix("#")
        val colorInt = if (cleanHex.length == 6) {
            (0xFF000000 or cleanHex.toLong(16)).toInt()
        } else {
            cleanHex.toLong(16).toInt()
        }
        Color(colorInt)
    } catch (_: Exception) {
        defaultColor
    }
}
