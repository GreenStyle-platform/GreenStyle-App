package com.vie.mit.green.ui.routeplanner.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mapbox.geojson.Point
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.vie.mit.common.screenbottomsheet.BottomSheetConfig
import com.vie.mit.common.screenbottomsheet.CommonScreenBottomSheet
import com.vie.mit.common.screenbottomsheet.CustomBottomSheetValue
import com.vie.mit.common.screenbottomsheet.rememberCustomBottomSheetState
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.common.ui.button.FloatingBackButton
import com.vie.mit.green.navigation.main_graph.RouteLocationPickerSheetRoute
import com.vie.mit.mapbox.composable.RoutePickerMap
import com.vie.mit.mapbox.util.focusUserLocationBoundingBox
import kotlin.math.roundToInt

/**
 * RoutePlannerContainerScreen: Container chính cho toàn bộ luồng tìm kiếm và hiển thị lộ trình.
 * - backgroundContent: Giữ duy nhất 1 Mapbox instance làm nền, vẽ đường nét đứt và cắm pin vị trí.
 * - sheetContent: Chứa Nested NavHost ([RoutePlannerNavHost]) chuyển đổi mượt mà giữa:
 *     1. [RouteLocationPickerSheetRoute]: Chọn điểm xuất phát & đích.
 *     2. [RouteOptionsRouteSheet]: Xem các phương án di chuyển.
 */
@Composable
fun RoutePlannerContainerScreen(
    containerViewModel: RoutePlannerContainerViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by containerViewModel.uiState.collectAsStateWithLifecycle()
    val routePlannerNavController = rememberNavController()
    val bottomSheetState = rememberCustomBottomSheetState()

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(106.7009, 10.7769))
            zoom(14.0)
        }
    }

    // Kiểm tra sheet hiện tại đang hiển thị
    val currentBackStackEntry by routePlannerNavController.currentBackStackEntryAsState()
    val isLocationPickerActive = remember(currentBackStackEntry) {
        currentBackStackEntry?.destination?.hasRoute<RouteLocationPickerSheetRoute>() != false
    }

    // Đồng bộ camera padding theo chiều cao thực tế của sheet (giống HomeContainerScreen)
    // Khi BottomSheet ở anchor PartiallyExpanded và kéo lên (Expanded), padding đáy giữ nguyên không đổi.
    // Khi BottomSheet hạ xuống (về Peeked/Hidden), cameraBottomPadding giảm mượt mà theo sheetHeightPx.
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

    // Lắng nghe sự kiện di chuyển camera từ Container
    LaunchedEffect(containerViewModel) {
        containerViewModel.cameraMoveEvent.collect { target ->
            mapViewportState.flyTo(
                cameraOptions = cameraOptions {
                    center(Point.fromLngLat(target.longitude, target.latitude))
                    zoom(15.0)
                },
                animationOptions = MapAnimationOptions.mapAnimationOptions {
                    duration(600)
                }
            )
        }
    }

    CommonScreenBottomSheet(
        initState = bottomSheetState,
        config = BottomSheetConfig(
            peekedRatio = 0.35f,
            partiallyExpandedRatio = 0.60f,
            expandedRatio = 0.92f,
        ),
        backgroundContent = {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Bản đồ Mapbox nền
                RoutePickerMap(
                    startPoint = uiState.startPoint,
                    endPoint = uiState.endPoint,
                    onCameraCenterChanged = containerViewModel::onMapCameraMoved,
                    cameraBottomPadding = cameraBottomPadding,
                    mapViewportState = mapViewportState,
                    onLocationUpdate = containerViewModel::onLocationUpdated,
                    centerOverlay = {
                        // Chỉ hiển thị Center Pin khi đang ở màn hình chọn vị trí
                        if (isLocationPickerActive) {
                            CenterLocationMarker(
                                isStart = uiState.focusedField == LocationPickerField.START,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // 2. Nút Back tròn nổi góc trên bên trái
                FloatingBackButton(
                    onClick = {
                        if (!isLocationPickerActive) {
                            routePlannerNavController.popBackStack()
                        } else {
                            containerViewModel.navigateBack()
                        }
                    },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 12.dp)
                        .align(Alignment.TopStart),
                )

                // 3. Nút "Chọn vị trí này trên bản đồ" (khi đang ở Sheet chọn điểm)
                if (isLocationPickerActive) {
                    ExtendedFloatingActionButton(
                        onClick = containerViewModel::onConfirmMapCenterLocation,
                        containerColor = if (uiState.focusedField == LocationPickerField.START) Color(0xFF10B981) else Color(0xFFEF4444),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = (cameraBottomPadding / 2.7f).coerceIn(80f, 320f).dp),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Chọn điểm này",
                                tint = Color.White,
                            )
                        },
                        text = {
                            Text(
                                text = if (uiState.focusedField == LocationPickerField.START) "Chọn làm Điểm đi" else "Chọn làm Điểm đến",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        },
                    )
                }

                // 4. Floating "My Location" Button (di chuyển mượt theo mép sheet)
                FloatingActionButton(
                    onClick = {
                        val latestLocation = containerViewModel.getLastKnownLocation() ?: uiState.currentLocation
                        if (latestLocation != null) {
                            containerViewModel.onUserLocationButtonClicked(latestLocation)
                            focusUserLocationBoundingBox(
                                mapViewportState = mapViewportState,
                                location = latestLocation,
                                zoom = 15.0,
                                animate = true,
                            )
                        }
                    },
                    shape = CircleShape,
                    containerColor = AppTheme.colors.white,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .offset {
                            IntOffset(0, -bottomSheetState.sheetHeightPx.roundToInt())
                        }
                        .size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Vị trí của tôi",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        sheetContent = {
            RoutePlannerNavHost(
                routePlannerNavController = routePlannerNavController,
                routePlannerContainerViewModel = containerViewModel,
                modifier = Modifier.fillMaxSize(),
            )
        },
        modifier = modifier.fillMaxSize(),
    )
}

/**
 * Center Marker nằm chính giữa viewport bản đồ:
 * - Hiển thị Icon Start (Xanh lá) khi đang focus ô Start.
 * - Hiển thị Icon End (Đỏ) khi đang focus ô End.
 */
@Composable
private fun CenterLocationMarker(
    isStart: Boolean,
    modifier: Modifier = Modifier,
) {
    val markerColor = if (isStart) Color(0xFF10B981) else Color(0xFFEF4444)
    val labelText = if (isStart) "Điểm xuất phát" else "Điểm đến"
    val icon = if (isStart) Icons.Default.Flag else Icons.Default.Place

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.offset(y = (-20).dp),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(markerColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = labelText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(markerColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = labelText,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Box(
            modifier = Modifier
                .size(6.dp)
                .offset(y = (-2).dp)
                .background(markerColor, CircleShape)
        )
    }
}
