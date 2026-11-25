package com.vie.mit.green.ui.routeplanner.routelocationpicker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.data.model.domain.map.AutosuggestPlace
import com.vie.mit.green.ui.routeplanner.container.LocationPickerField
import com.vie.mit.green.ui.routeplanner.container.RoutePlannerContainerViewModel

/**
 * RouteLocationPickerSheet: Nội dung Bottom Sheet thứ nhất trong RoutePlanner.
 * Cho phép người dùng nhập tìm kiếm điểm đi / điểm đến bằng text hoặc kết hợp chọn trên bản đồ.
 */
@Composable
fun RouteLocationPickerSheet(
    containerViewModel: RoutePlannerContainerViewModel,
    onNavigateToRouteOptions: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by containerViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Điểm xuất phát",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.focusedField == LocationPickerField.START) Color(0xFF10B981) else Color(0xFF6B7280),
                        )
                        BasicTextField(
                            value = uiState.startQuery,
                            onValueChange = { query ->
                                containerViewModel.onQueryChanged(
                                    field = LocationPickerField.START,
                                    newQuery = query,
                                )
                            },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937),
                            ),
                            cursorBrush = SolidColor(Color(0xFF10B981)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        containerViewModel.setFocusedField(LocationPickerField.START)
                                    }
                                },
                            decorationBox = { innerTextField ->
                                if (uiState.startQuery.isEmpty()) {
                                    Text(
                                        text = "Chọn điểm xuất phát...",
                                        style = TextStyle(fontSize = 14.sp, color = Color(0xFF9CA3AF)),
                                    )
                                }
                                innerTextField()
                            },
                        )
                    }

                    if (uiState.startQuery.isNotEmpty()) {
                        IconButton(
                            onClick = containerViewModel::clearStartLocation,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa điểm xuất phát",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Divider + Nút Swap đảo vị trí
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 11.dp)
                            .width(2.dp)
                            .height(20.dp)
                            .background(Color(0xFFE5E7EB))
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = containerViewModel::swapLocations,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Đổi vị trí",
                            tint = AppTheme.colors.navyBlue,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // 2. Ô nhập điểm đích (End)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFFFEBEE), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFEF4444), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Điểm đến",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.focusedField == LocationPickerField.END) Color(0xFFEF4444) else Color(0xFF6B7280),
                        )
                        BasicTextField(
                            value = uiState.endQuery,
                            onValueChange = { query ->
                                containerViewModel.onQueryChanged(
                                    field = LocationPickerField.END,
                                    newQuery = query,
                                )
                            },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937),
                            ),
                            cursorBrush = SolidColor(Color(0xFFEF4444)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        containerViewModel.setFocusedField(LocationPickerField.END)
                                    }
                                },
                            decorationBox = { innerTextField ->
                                if (uiState.endQuery.isEmpty()) {
                                    Text(
                                        text = "Chọn điểm đến...",
                                        style = TextStyle(fontSize = 14.sp, color = Color(0xFF9CA3AF)),
                                    )
                                }
                                innerTextField()
                            },
                        )
                    }

                    if (uiState.endQuery.isNotEmpty()) {
                        IconButton(
                            onClick = containerViewModel::clearEndLocation,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa điểm đến",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Nút "Tìm tuyến đường" hiển thị khi cả 2 điểm đã được chọn
        AnimatedVisibility(
            visible = uiState.hasValidRoute,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Button(
                onClick = onNavigateToRouteOptions,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.navyBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsTransit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Xem phương án di chuyển",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // ==============================================================================
        // Danh sách kết quả gợi ý địa điểm (Autosuggest & Autocomplete)
        // ==============================================================================
        if (uiState.isLoadingSuggestions) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        } else if (uiState.suggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = uiState.suggestions,
                    key = { it.id },
                ) { place ->
                    SuggestionPlaceItem(
                        place = place,
                        onClick = { containerViewModel.onSelectSuggestion(place) },
                    )
                }
            }
        } else {
            // Gợi ý hướng dẫn khi danh sách trống
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Kéo bản đồ để chọn vị trí trực tiếp hoặc nhập từ khóa tìm kiếm địa điểm.",
                        style = AppTheme.typography.bodyMedium.copy(color = Color.White),
                    )
                }
            }
        }
    }

/**
 * Item hiển thị địa điểm trong danh sách gợi ý.
 */
@Composable
private fun SuggestionPlaceItem(
    place: AutosuggestPlace,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFF3F4F6), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = AppTheme.colors.navyBlue,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.title,
                    style = AppTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (place.address.isNotBlank() && place.address != place.title) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = place.address,
                        style = AppTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val dist = place.distance
            if (dist != null && dist > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                val distanceText = if (dist >= 1000) {
                    String.format("%.1f km", dist / 1000.0)
                } else {
                    "$dist m"
                }
                Text(
                    text = distanceText,
                    style = AppTheme.typography.bodySmall.copy(
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
