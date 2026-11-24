package com.vie.mit.green.ui.transitroutedetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.vie.mit.common.ui.button.FloatingBackButton
import com.vie.mit.data.model.domain.map.RoundTrip
import com.vie.mit.data.model.domain.map.TransitStopMarker
import com.vie.mit.mapbox.composable.TransitRouteFocusMap

/**
 * Screen displaying the detailed route view with Mapbox path, stops, and bottom sheet.
 */
@Composable
fun TransitRouteDetailScreen(
    viewModel: TransitRouteDetailViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSheetState = rememberCustomBottomSheetState(
        initialValue = CustomBottomSheetValue.PartiallyExpanded,
    )

    val sheetConfig = remember {
        BottomSheetConfig(
            peekedRatio = 0.22f,
            partiallyExpandedRatio = 0.65f,
            expandedRatio = 1.0f,
        )
    }

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

    CommonScreenBottomSheet(
        modifier = modifier.fillMaxSize(),
        initState = bottomSheetState,
        config = sheetConfig,
        backgroundContent = { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Mapbox path & stops
                TransitRouteFocusMap(
                    shapes = uiState.shapes,
                    stops = uiState.stops,
                    cameraBottomPadding = cameraBottomPadding,
                )

                // Top Floating Back Button
                FloatingBackButton(
                    onClick = viewModel::navigateBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 16.dp, start = 16.dp),
                )
            }
        },
        sheetContent = {
            TransitRouteDetailSheetContent(
                uiState = uiState,
                onDirectionSwap = {
                    val nextDirection = if (uiState.selectedDirectionId == 0) 1 else 0
                    viewModel.onSelectDirection(nextDirection)
                },
                onSelectRoundTrip = { roundTrip ->
                    viewModel.onSelectService(roundTrip.serviceId)
                },
                onRetry = viewModel::loadRouteTrips,
            )
        },
    )
}

/**
 * Backward compatibility alias for [TransitRouteDetailScreen].
 */
@Composable
fun TransitRoutePathScreen(
    viewModel: TransitRouteDetailViewModel,
    modifier: Modifier = Modifier,
) {
    TransitRouteDetailScreen(viewModel = viewModel, modifier = modifier)
}

@Composable
private fun TransitRouteDetailSheetContent(
    uiState: TransitRouteDetailUiState,
    onDirectionSwap: () -> Unit,
    onSelectRoundTrip: (RoundTrip) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val busColor = AppTheme.colors.busColor
    val activeRoundTrip = uiState.selectedRoundTrip
    val selectedTrip = uiState.selectedTrip

    val startTerminal = remember(uiState.stops, activeRoundTrip, uiState.selectedDirectionId) {
        if (uiState.stops.isNotEmpty()) {
            uiState.stops.first().name
        } else {
            if (uiState.selectedDirectionId == 0) {
                activeRoundTrip?.inboundTrip?.tripHeadsign ?: "Điểm xuất phát"
            } else {
                activeRoundTrip?.outboundTrip?.tripHeadsign ?: "Điểm xuất phát"
            }
        }
    }

    val endTerminal = remember(uiState.stops, selectedTrip, activeRoundTrip) {
        if (uiState.stops.isNotEmpty()) {
            uiState.stops.last().name
        } else {
            selectedTrip?.tripHeadsign ?: "Điểm kết thúc"
        }
    }

    val directionLabel = if (uiState.selectedDirectionId == 0) "Chiều đi" else "Chiều về"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp)
                .size(width = 36.dp, height = 4.dp)
                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = busColor)
            }
            return
        }

        if (uiState.error != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = uiState.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = busColor),
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thử lại")
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Route Header Card with Direction Swap
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Route short name badge
                            Box(
                                modifier = Modifier
                                    .background(busColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = activeRoundTrip?.routeShortName.orEmpty().ifBlank { "Bus" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Route Long Name
                            Text(
                                text = activeRoundTrip?.routeLongName.orEmpty(),
                                style = AppTheme.typography.headlineMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1F2937),
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )

                            // Direction Swap Button
                            IconButton(
                                onClick = onDirectionSwap,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF3F4F6), CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Đổi chiều đi/về",
                                    tint = busColor,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Direction Tag + Start & End Terminals
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = busColor.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = directionLabel,
                                    color = busColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = startTerminal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF374151),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(14.dp),
                                tint = Color(0xFF9CA3AF),
                            )

                            Text(
                                text = endTerminal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF374151),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                }
            }

            // 2. Round Trips Section (Lịch trình chạy)
            if (uiState.roundTrips.isNotEmpty()) {
                item {
                    Column {

                            Text(
                                text = "Lịch trình chuyến (${uiState.roundTrips.size})",
                                style = AppTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppTheme.colors.white,
                                ),
                            )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                        ) {
                            items(uiState.roundTrips) { roundTrip ->
                                val isSelected = roundTrip.serviceId == uiState.selectedServiceId
                                val containerColor by animateColorAsState(
                                    targetValue = if (isSelected) busColor else Color.White,
                                    label = "roundTripBg",
                                )
                                val textColor by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else Color(0xFF374151),
                                    label = "roundTripText",
                                )

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectRoundTrip(roundTrip) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = containerColor,
                                    shadowElevation = if (isSelected) 3.dp else 1.dp,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color(0xFFE5E7EB),
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        Column {
                                            Text(
                                                text = roundTrip.serviceId,
                                                color = textColor,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Stops Timeline List
            item {
                    Text(
                        text = "Danh sách điểm dừng (${uiState.stops.size})",
                        style = AppTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.white,
                        ),
                    )

                    if (uiState.isStopsLoading || uiState.isShapeLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = busColor,
                        )
                    }
            }

            if (uiState.stops.isEmpty() && !uiState.isStopsLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Không có điểm dừng nào cho chuyến này",
                                color = Color(0xFF6B7280),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(uiState.stops) { index, stop ->
                    StopTimelineItem(
                        index = index,
                        totalStops = uiState.stops.size,
                        stop = stop,
                        accentColor = busColor,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StopTimelineItem(
    index: Int,
    totalStops: Int,
    stop: TransitStopMarker,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val isFirst = index == 0
    val isLast = index == totalStops - 1

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sequence dot / icon
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (isFirst || isLast) accentColor else Color(0xFFE5E7EB),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${index + 1}",
                color = if (isFirst || isLast) Color.White else Color(0xFF4B5563),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.name,
                style = AppTheme.typography.bodyMedium.copy(
                    fontWeight = if (isFirst || isLast) FontWeight.Bold else FontWeight.Normal,
                    color = Color(0xFF1F2937),
                    fontSize = 14.sp,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (isFirst || isLast) {
                Text(
                    text = if (isFirst) "Điểm đầu" else "Điểm cuối",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
