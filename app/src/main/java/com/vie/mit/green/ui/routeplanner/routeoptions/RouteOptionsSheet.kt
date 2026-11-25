package com.vie.mit.green.ui.routeplanner.routeoptions

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.green.ui.routeplanner.container.RoutePlannerContainerViewModel
import com.vie.mit.mapbox.model.distanceTo

data class RouteOptionItem(
    val id: String,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val costVnd: Int,
    val walkDistanceMeters: Int,
    val icon: ImageVector,
    val iconColor: Color,
    val tags: List<String> = emptyList(),
)

/**
 * RouteOptionsSheet: Nội dung Bottom Sheet thứ hai trong RoutePlanner.
 * Hiển thị danh sách các phương án di chuyển giữa điểm xuất phát và điểm đích đã chọn.
 */
@Composable
fun RouteOptionsSheet(
    containerViewModel: RoutePlannerContainerViewModel,
    onBackToPicker: () -> Unit,
    modifier: Modifier = Modifier,
    onOptionSelected: (RouteOptionItem) -> Unit = {},
) {
    val uiState by containerViewModel.uiState.collectAsStateWithLifecycle()
    val selectedFilter = uiState.selectedRouteFilter

    // Khoảng cách đường thẳng ước tính
    val directDistanceKm = remember(uiState.startPoint, uiState.endPoint) {
        val start = uiState.startPoint
        val end = uiState.endPoint
        if (start != null && end != null) {
            String.format("%.1f km", start.distanceTo(end) / 1000.0)
        } else {
            "-- km"
        }
    }

    // Danh sách mẫu các options di chuyển
    val options = remember(uiState.startAddress, uiState.endAddress) {
        listOf(
            RouteOptionItem(
                id = "opt_bus_fast",
                title = "Xe buýt tuyến 01",
                description = "Bến Thành ➔ Chợ Lớn • Chuyến kế tiếp trong 5 phút",
                durationMinutes = 28,
                costVnd = 7000,
                walkDistanceMeters = 250,
                icon = Icons.Default.DirectionsBus,
                iconColor = Color(0xFF10B981),
                tags = listOf("Nhanh nhất", "Tiết kiệm"),
            ),
            RouteOptionItem(
                id = "opt_metro_1",
                title = "Tàu điện Metro Tuyến 1",
                description = "Ga Bến Thành ➔ Ga Nhà hát Thành phố",
                durationMinutes = 18,
                costVnd = 12000,
                walkDistanceMeters = 400,
                icon = Icons.Default.Train,
                iconColor = Color(0xFF3B82F6),
                tags = listOf("Đúng giờ", "Tránh kẹt xe"),
            ),
            RouteOptionItem(
                id = "opt_bike",
                title = "Xe đạp công cộng TNGo",
                description = "Thuê tại trạm gần nhất • Tuyến đường ven sông",
                durationMinutes = 25,
                costVnd = 10000,
                walkDistanceMeters = 100,
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                iconColor = Color(0xFFF59E0B),
                tags = listOf("Thân thiện môi trường"),
            ),
            RouteOptionItem(
                id = "opt_walk",
                title = "Đi bộ",
                description = "Tuyến đường đi bộ an toàn có vỉa hè",
                durationMinutes = 48,
                costVnd = 0,
                walkDistanceMeters = 3200,
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                iconColor = Color(0xFF6B7280),
                tags = listOf("Tập luyện"),
            ),
        )
    }

    val filteredOptions = remember(selectedFilter, options) {
        when (selectedFilter) {
            "Xe buýt" -> options.filter { it.icon == Icons.Default.DirectionsBus }
            "Tàu điện" -> options.filter { it.icon == Icons.Default.Train }
            "Xe đạp" -> options.filter { it.icon == Icons.AutoMirrored.Filled.DirectionsBike }
            "Đi bộ" -> options.filter { it.icon == Icons.AutoMirrored.Filled.DirectionsWalk }
            else -> options
        }
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
                .padding(vertical = 6.dp)
                .size(width = 40.dp, height = 4.dp)
                .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
        )

        // Header: Nút Back + Tiêu đề
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            IconButton(onClick = onBackToPicker) {
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
            )
        }

        // Tóm tắt Lộ trình Điểm xuất phát ➔ Điểm đích
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), CircleShape)
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFEF4444), CircleShape)
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

                IconButton(onClick = onBackToPicker) {
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

        // Filter chips row
        val filters = listOf("Tất cả", "Xe buýt", "Tàu điện", "Xe đạp", "Đi bộ")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { containerViewModel.onSelectRouteFilter(filter) },
                    label = {
                        Text(
                            text = filter,
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

        // Danh sách các phương án di chuyển
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = filteredOptions,
                key = { it.id },
            ) { option ->
                RouteOptionCard(
                    option = option,
                    onClick = { onOptionSelected(option) },
                )
            }
        }
    }
}

@Composable
private fun RouteOptionCard(
    option: RouteOptionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Icon phương tiện
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(option.iconColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = option.iconColor,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.title,
                        style = AppTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            fontSize = 16.sp,
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = option.description,
                        style = AppTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Thời gian ước tính nổi bật
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${option.durationMinutes} phút",
                        style = AppTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                        ),
                    )
                    if (option.costVnd > 0) {
                        Text(
                            text = "${String.format("%,d", option.costVnd)} đ",
                            style = AppTheme.typography.bodySmall.copy(
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    } else {
                        Text(
                            text = "Miễn phí",
                            style = AppTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280),
                            ),
                        )
                    }
                }
            }

            // Tags (ví dụ: Nhanh nhất, Đúng giờ)
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
