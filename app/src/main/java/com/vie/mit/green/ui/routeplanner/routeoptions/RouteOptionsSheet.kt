package com.vie.mit.green.ui.routeplanner.routeoptions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vie.mit.common.theme.AppColors
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.domain.map.RouteLeg
import com.vie.mit.data.model.domain.map.RouteOption
import com.vie.mit.green.ui.routeplanner.components.DepartureTimePickerDialog
import com.vie.mit.green.ui.routeplanner.components.DepartureTimeSelectorBar
import com.vie.mit.green.ui.routeplanner.container.LocationPickerField
import com.vie.mit.green.ui.routeplanner.container.RouteOptionFilter
import com.vie.mit.green.ui.routeplanner.container.RoutePlannerContainerViewModel
import com.vie.mit.green.ui.routeplanner.container.toRouteColor
import com.vie.mit.mapbox.model.distanceTo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RouteOptionsSheet: Nội dung Bottom Sheet thứ hai trong RoutePlanner.
 * Hiển thị danh sách các phương án di chuyển giữa điểm xuất phát và điểm đích từ OTP2 Backend.
 */
@Composable
fun RouteOptionsSheet(
    containerViewModel: RoutePlannerContainerViewModel,
    onBackToPicker: (fieldToFocus: LocationPickerField?) -> Unit,
    modifier: Modifier = Modifier,
    onOptionSelected: (RouteOption) -> Unit = {},
) {
    val uiState by containerViewModel.uiState.collectAsStateWithLifecycle()
    val selectedFilter = uiState.selectedRouteFilter
    var showDepartureTimeDialog by remember { mutableStateOf(false) }

    // Đảm bảo dữ liệu được tải nếu chưa có khi mở trực tiếp sheet
    LaunchedEffect(uiState.startPoint, uiState.endPoint) {
        if (
            uiState.hasValidRoute &&
            uiState.routeOptions.isEmpty() &&
            !uiState.isLoadingRoutes &&
            uiState.routeOptionsError == null
        ) {
            containerViewModel.loadRouteOptions()
        }
    }

    val directDistanceKm = remember(uiState.startPoint, uiState.endPoint) {
        val start = uiState.startPoint
        val end = uiState.endPoint
        if (start != null && end != null) {
            String.format(Locale.US, "%.1f km", start.distanceTo(end) / 1000.0)
        } else {
            null
        }
    }

    val filteredOptions = remember(selectedFilter, uiState.routeOptions) {
        uiState.filteredRouteOptions
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
    ) {
        // Thanh kéo Sheet Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 4.dp)
                .size(width = 40.dp, height = 4.dp)
                .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(2.dp)),
        )

        // Header: Nút Back + Tiêu đề + Khoảng cách ước tính
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        ) {
            IconButton(onClick = { onBackToPicker(null) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại chỉnh sửa điểm",
                    tint = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Phương án di chuyển",
                style = AppTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp,
                ),
                modifier = Modifier.weight(1f),
            )
            if (directDistanceKm != null) {
                Text(
                    text = directDistanceKm,
                    style = AppTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }

        // Tóm tắt Lộ trình Điểm xuất phát ➔ Điểm đích (nhấn vào từng dòng để sửa nhanh)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBackToPicker(LocationPickerField.START) },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), CircleShape),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.startAddress.ifEmpty { "Điểm xuất phát" },
                            style = AppTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937),
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBackToPicker(LocationPickerField.END) },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFEF4444), CircleShape),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.endAddress.ifEmpty { "Điểm đến" },
                            style = AppTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937),
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                IconButton(onClick = { onBackToPicker(null) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Chỉnh sửa",
                        tint = AppTheme.colors.navyBlue,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Thanh thời điểm xuất phát (chạm để đổi giờ khởi hành và tải lại phương án ngay)
        DepartureTimeSelectorBar(
            selectedDepartureTimeMillis = uiState.selectedDepartureTimeMillis,
            onClick = { showDepartureTimeDialog = true },
            onResetToNow = {
                containerViewModel.onSelectDepartureTime(
                    departureTimeMillis = null,
                    reloadIfValidRoute = true,
                )
            },
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips row (cuộn ngang được cho mọi kích thước màn hình)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(
                items = RouteOptionFilter.entries,
                key = { it.name },
            ) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { containerViewModel.onSelectRouteFilter(filter) },
                    label = {
                        Text(
                            text = filter.toLabel(),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = AppTheme.colors.navyBlue,
                        containerColor = Color.White.copy(alpha = 0.15f),
                        labelColor = Color.White,
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Danh sách các phương án di chuyển (Loading / Error / Empty / Content)
        when {
            uiState.isLoadingRoutes -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            uiState.routeOptionsError != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Text(
                        text = uiState.routeOptionsError ?: "Đã có lỗi xảy ra",
                        style = AppTheme.typography.bodyMedium.copy(color = Color.White),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { containerViewModel.loadRouteOptions() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    ) {
                        Text(
                            text = "Thử lại",
                            color = AppTheme.colors.navyBlue,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            filteredOptions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val emptyMessage = if (uiState.routeOptions.isEmpty()) {
                        "Không tìm thấy tuyến giao thông công cộng phù hợp."
                    } else {
                        "Không có phương án phù hợp với bộ lọc đã chọn."
                    }
                    Text(
                        text = emptyMessage,
                        style = AppTheme.typography.bodyMedium.copy(color = Color.White),
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = filteredOptions,
                        key = { it.id },
                    ) { option ->
                        RouteOptionCard(
                            option = option,
                            isSelected = uiState.selectedRouteOptionId == option.id,
                            onClick = {
                                containerViewModel.onSelectRouteOption(option.id)
                                onOptionSelected(option)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDepartureTimeDialog) {
        DepartureTimePickerDialog(
            initialDepartureTimeMillis = uiState.selectedDepartureTimeMillis,
            onDismiss = { showDepartureTimeDialog = false },
            onConfirm = { selectedMillis ->
                showDepartureTimeDialog = false
                containerViewModel.onSelectDepartureTime(
                    departureTimeMillis = selectedMillis,
                    reloadIfValidRoute = true,
                )
            },
        )
    }
}

private fun RouteOptionFilter.toLabel(): String = when (this) {
    RouteOptionFilter.ALL -> "Tất cả"
    RouteOptionFilter.BUS -> "Xe buýt"
    RouteOptionFilter.SUBWAY -> "Tàu điện"
    RouteOptionFilter.BIKE -> "Xe đạp"
    RouteOptionFilter.WALK -> "Đi bộ"
}

private fun TransitMode.toIconAndColor(colors: AppColors): Pair<ImageVector, Color> = when (this) {
    TransitMode.BUS, TransitMode.TROLLEYBUS -> Icons.Default.DirectionsBus to toRouteColor(colors)
    TransitMode.SUBWAY, TransitMode.MONORAIL -> Icons.Default.DirectionsSubway to toRouteColor(
        colors
    )
    TransitMode.RAIL, TransitMode.TRAM, TransitMode.CABLE_TRAM, TransitMode.AERIAL_LIFT -> Icons.Default.Train to toRouteColor(
        colors
    )

    TransitMode.FERRY -> Icons.Default.DirectionsBoat to toRouteColor(colors)
    TransitMode.BIKE -> Icons.AutoMirrored.Filled.DirectionsBike to toRouteColor(colors)
    TransitMode.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk to toRouteColor(colors)
}

private fun formatDurationLabel(durationMinutes: Int): String {
    if (durationMinutes < 60) return "$durationMinutes phút"
    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60
    return if (minutes == 0) "${hours}g" else "${hours}g ${minutes}p"
}

private fun formatTimeRange(startMillis: Long?, endMillis: Long?): String? {
    if (startMillis == null || endMillis == null) return null
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${formatter.format(Date(startMillis))} – ${formatter.format(Date(endMillis))}"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteOptionCard(
    option: RouteOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val (icon, iconColor) = remember(option.transportType, colors) {
        option.transportType.toIconAndColor(colors)
    }
    val timeRangeText = remember(option.startTimeMillis, option.endTimeMillis) {
        formatTimeRange(option.startTimeMillis, option.endTimeMillis)
    }
    val firstTransitLeg = remember(option.legs) {
        option.legs.firstOrNull { it.mode.isTransit }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (isSelected) BorderStroke(2.dp, AppTheme.colors.navyBlue) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            // Hàng 1: Chuỗi các chặng đa phương thức + Tổng thời lượng
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (option.legs.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        option.legs.forEachIndexed { index, leg ->
                            RouteLegBadge(leg = leg)
                            if (index < option.legs.lastIndex) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.CenterVertically),
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(iconColor.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = option.title,
                            style = AppTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937),
                                fontSize = 15.sp,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDurationLabel(option.durationMinutes),
                        style = AppTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            fontSize = 16.sp,
                        ),
                    )
                    val cost = option.costVnd
                    if (cost != null && cost > 0) {
                        Text(
                            text = "${String.format(Locale.US, "%,d", cost)} đ",
                            style = AppTheme.typography.bodySmall.copy(
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hàng 2: Khung giờ khởi hành – đến nơi & mô tả chuyển tuyến / đi bộ
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (timeRangeText != null) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = AppTheme.colors.navyBlue,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeRangeText,
                        style = AppTheme.typography.bodySmall.copy(
                            color = Color(0xFF1F2937),
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        style = AppTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF)),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = option.description,
                    style = AppTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            // Hàng 3: Gợi ý điểm đón / hướng tuyến đầu tiên (nếu có)
            val boardingSummary = remember(firstTransitLeg) {
                val leg = firstTransitLeg ?: return@remember null
                val stopName = leg.from?.name?.takeIf { it.isNotBlank() && it != "Origin" }
                val headsign = leg.headsign?.takeIf { it.isNotBlank() }
                when {
                    stopName != null && headsign != null -> "Đón tại $stopName • Hướng $headsign"
                    stopName != null -> "Đón tại $stopName"
                    headsign != null -> "Hướng $headsign"
                    else -> null
                }
            }
            if (boardingSummary != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = boardingSummary,
                    style = AppTheme.typography.bodySmall.copy(
                        color = Color(0xFF4B5563),
                        fontSize = 12.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Hàng 4: Nhãn tóm tắt (Nhanh nhất, Đi thẳng, Ít đi bộ nhất)
            if (option.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    option.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4B5563),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteLegBadge(
    leg: RouteLeg,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val (icon, color) = remember(leg.mode, colors) { leg.mode.toIconAndColor(colors) }
    val isTransit = leg.mode.isTransit
    val label = if (isTransit) {
        leg.routeShortName?.takeIf { it.isNotBlank() } ?: "${leg.durationMinutes}p"
    } else {
        "${leg.durationMinutes}p"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = if (isTransit) color else Color(0xFFF3F4F6),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 7.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isTransit) Color.White else color,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isTransit) Color.White else Color(0xFF4B5563),
            maxLines = 1,
        )
    }
}
