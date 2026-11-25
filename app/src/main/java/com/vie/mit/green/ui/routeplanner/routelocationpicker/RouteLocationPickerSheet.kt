package com.vie.mit.green.ui.routeplanner.routelocationpicker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.common.ui.textfield.LocationInputTextFieldGre
import com.vie.mit.data.model.domain.map.AutosuggestPlace
import com.vie.mit.green.ui.routeplanner.container.LocationPickerField
import com.vie.mit.green.ui.routeplanner.container.RoutePlannerContainerViewModel
import java.util.Locale

private val InputBoxHeight = 46.dp
private val InputBoxSpacing = 10.dp

/**
 * RouteLocationPickerSheet: Nội dung Bottom Sheet thứ nhất trong RoutePlanner.
 * Cho phép người dùng nhập tìm kiếm điểm đi / điểm đến bằng text hoặc kết hợp kéo chọn trên bản đồ.
 */
@Composable
fun RouteLocationPickerSheet(
    containerViewModel: RoutePlannerContainerViewModel,
    onSearchFieldClicked: () -> Unit = {},
    onPickOnMapClicked: () -> Unit = {},
    onNavigateToRouteOptions: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by containerViewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val keyboardController = LocalSoftwareKeyboardController.current

    val startFocusRequester = remember { FocusRequester() }
    val endFocusRequester = remember { FocusRequester() }
    val startInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource = remember { MutableInteractionSource() }

    var swapRotationTarget by remember { mutableFloatStateOf(0f) }
    val swapRotation by animateFloatAsState(
        targetValue = swapRotationTarget,
        animationSpec = tween(durationMillis = 250),
        label = "swap_icon_rotation",
    )

    // Khi người dùng nhấn trực tiếp vào TextField (kể cả khi field đó đã có focus lúc sheet đang Peeked)
    LaunchedEffect(startInteractionSource) {
        startInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                containerViewModel.setFocusedField(LocationPickerField.START)
                onSearchFieldClicked()
            }
        }
    }

    LaunchedEffect(endInteractionSource) {
        endInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                containerViewModel.setFocusedField(LocationPickerField.END)
                onSearchFieldClicked()
            }
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
                .padding(top = 2.dp, bottom = 10.dp)
                .size(width = 40.dp, height = 4.dp)
                .background(colors.white.copy(alpha = 0.45f), RoundedCornerShape(2.dp)),
        )

        // ==============================================================================
        // Card nhập Điểm xuất phát (Start) & Điểm đến (End) — Chuẩn UI/UX Citymapper
        // Bố cục 3 cột: Timeline Indicator (trái) | 2 Ô nhập liệu (giữa) | Nút Swap (phải)
        // ==============================================================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.white),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {


                // Cột 2: 2 ô nhập liệu Điểm xuất phát & Điểm đến dùng component chung LocationInputTextFieldGre
                val isStartCurrentLocation =
                    uiState.startQuery == RoutePlannerContainerViewModel.CURRENT_LOCATION_LABEL
                val isEndCurrentLocation =
                    uiState.endQuery == RoutePlannerContainerViewModel.CURRENT_LOCATION_LABEL

                Column(
                    verticalArrangement = Arrangement.spacedBy(InputBoxSpacing),
                    modifier = Modifier.weight(1f),
                ) {
                    LocationInputTextFieldGre(
                        value = uiState.startQuery,
                        onValueChange = { query ->
                            containerViewModel.onQueryChanged(
                                field = LocationPickerField.START,
                                newQuery = query,
                            )
                        },
                        placeholder = "Điểm xuất phát...",
                        isActive = uiState.focusedField == LocationPickerField.START,
                        accentColor = colors.routeStartColor,
                        leadingIcon = if (isStartCurrentLocation) Icons.Default.MyLocation else null,
                        leadingIconTint = colors.routeStartColor,
                        textColor = if (isStartCurrentLocation) colors.routeStartTextColor else colors.primaryText,
                        height = InputBoxHeight,
                        focusRequester = startFocusRequester,
                        interactionSource = startInteractionSource,
                        imeAction = ImeAction.Next,
                        onImeAction = {
                            containerViewModel.setFocusedField(LocationPickerField.END)
                            endFocusRequester.requestFocus()
                        },
                        onFocused = {
                            containerViewModel.setFocusedField(LocationPickerField.START)
                            onSearchFieldClicked()
                        },
                        onClick = {
                            containerViewModel.setFocusedField(LocationPickerField.START)
                            onSearchFieldClicked()
                            startFocusRequester.requestFocus()
                        },
                        onClear = {
                            containerViewModel.clearStartLocation()
                            onSearchFieldClicked()
                            startFocusRequester.requestFocus()
                        },
                    )

                    LocationInputTextFieldGre(
                        value = uiState.endQuery,
                        onValueChange = { query ->
                            containerViewModel.onQueryChanged(
                                field = LocationPickerField.END,
                                newQuery = query,
                            )
                        },
                        placeholder = "Bạn muốn đi đâu?",
                        isActive = uiState.focusedField == LocationPickerField.END,
                        accentColor = colors.routeEndColor,
                        leadingIcon = if (isEndCurrentLocation) Icons.Default.MyLocation else null,
                        leadingIconTint = colors.routeStartColor,
                        textColor = if (isEndCurrentLocation) colors.routeStartTextColor else colors.primaryText,
                        height = InputBoxHeight,
                        focusRequester = endFocusRequester,
                        interactionSource = endInteractionSource,
                        imeAction = ImeAction.Search,
                        onImeAction = {
                            keyboardController?.hide()
                        },
                        onFocused = {
                            containerViewModel.setFocusedField(LocationPickerField.END)
                            onSearchFieldClicked()
                        },
                        onClick = {
                            containerViewModel.setFocusedField(LocationPickerField.END)
                            onSearchFieldClicked()
                            endFocusRequester.requestFocus()
                        },
                        onClear = {
                            containerViewModel.clearEndLocation()
                            onSearchFieldClicked()
                            endFocusRequester.requestFocus()
                        },
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Cột 3: Nút đảo chiều Start ↔ End căn giữa theo chiều dọc
                Surface(
                    onClick = {
                        swapRotationTarget += 180f
                        containerViewModel.swapLocations()
                    },
                    shape = CircleShape,
                    color = colors.inputContainerUnfocused,
                    border = BorderStroke(1.dp, colors.inputBorderUnfocused),
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Đổi vị trí điểm đi và điểm đến",
                            tint = colors.navyBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(swapRotation),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Thanh hành động nhanh kiểu Citymapper: Vị trí hiện tại & Chọn trên bản đồ
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.currentLocation != null) {
                QuickActionChip(
                    icon = Icons.Default.MyLocation,
                    label = "Vị trí hiện tại",
                    onClick = containerViewModel::useCurrentLocationForFocusedField,
                    modifier = Modifier.weight(1f),
                )
            }
            QuickActionChip(
                icon = Icons.Default.Map,
                label = "Chọn trên bản đồ",
                onClick = onPickOnMapClicked,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Nút "Xem phương án di chuyển" hiển thị khi cả 2 điểm đã được chọn
        AnimatedVisibility(
            visible = uiState.hasValidRoute,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Button(
                onClick = onNavigateToRouteOptions,
                colors = ButtonDefaults.buttonColors(containerColor = colors.navyBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsTransit,
                    contentDescription = null,
                    tint = colors.white,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Xem phương án di chuyển",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.white,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.white,
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
                    color = colors.white,
                    modifier = Modifier.size(28.dp),
                )
            }
        } else if (uiState.suggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding(),
                contentPadding = PaddingValues(bottom = 12.dp),
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
                colors = CardDefaults.cardColors(containerColor = colors.white.copy(alpha = 0.14f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable(onClick = onPickOnMapClicked),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = colors.white,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Nhấn vào bản đồ hoặc kéo chọn trực tiếp để ghim vị trí, hoặc nhập từ khóa tìm kiếm địa điểm.",
                        style = AppTheme.typography.bodyMedium.copy(color = colors.white),
                    )
                }
            }
        }
    }
}

/**
 * Cột chỉ báo trực quan tuyến đường bên trái 2 ô nhập liệu:
 * - Chấm xanh lá căn chính giữa ô Điểm xuất phát (centerY = 23.dp).
 * - 3 chấm nối dọc nằm giữa 2 điểm.
 * - Icon ghim đỏ căn chính giữa ô Điểm đến (centerY = 79.dp).
 */
@Composable
private fun RouteEndpointsIndicator(
    isStartFocused: Boolean,
    isEndFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val startHaloAlpha = if (isStartFocused) 1f else 0.55f
    val endTint = if (isEndFocused) colors.routeEndColor else colors.routeEndColor.copy(alpha = 0.8f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(22.dp)
            .height(InputBoxHeight * 2 + InputBoxSpacing),
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Start indicator (18.dp -> center tại 23.dp khớp giữa ô Start 46.dp)
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(colors.routeStartHaloColor.copy(alpha = startHaloAlpha), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colors.routeStartColor, CircleShape)
                    .border(1.5.dp, colors.white, CircleShape),
            )
        }

        // 3 chấm nối giữa Start và End (chiều cao 37.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .height(37.dp)
                .padding(vertical = 4.dp),
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .background(colors.routeConnectorDot, CircleShape),
                )
            }
        }

        // End indicator (20.dp -> center tại 79.dp khớp giữa ô End 46.dp)
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = endTint,
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.height(13.dp))
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = colors.white.copy(alpha = 0.18f),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.white,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.white),
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
                    .background(colors.inputContainerUnfocused, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = colors.navyBlue,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.title,
                    style = AppTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (place.address.isNotBlank() && place.address != place.title) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = place.address,
                        style = AppTheme.typography.bodySmall.copy(color = colors.secondaryText),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val dist = place.distance
            if (dist != null && dist > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                val distanceText = if (dist >= 1000) {
                    String.format(Locale.US, "%.1f km", dist / 1000.0)
                } else {
                    "$dist m"
                }
                Text(
                    text = distanceText,
                    style = AppTheme.typography.bodySmall.copy(
                        color = colors.inputPlaceholder,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
