package com.vie.mit.green.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.vie.mit.common.screenbottomsheet.CommonScreenBottomSheet
import com.vie.mit.common.screenbottomsheet.rememberCustomBottomSheetState
import com.vie.mit.green.ui.home.homesheet.StopDetailFloatingCard
import com.vie.mit.mapbox.composable.TransitStopsMapScreen
import com.vie.mit.mapbox.util.focusUserLocationBoundingBox

/**
 * MapContainerScreen: Container chính chứa duy nhất 1 Mapbox instance ở background
 * và một BottomSheet với Nested NavHost để hiển thị nội dung các màn hình dạng Sheet.
 *
 * - Mapbox chỉ khởi tạo 1 lần duy nhất, không bị destroy/recreate khi navigate.
 * - Khi chuyển Sheet (navigate), chỉ cập nhật state cho Map (camera, layer...) mà không reload data.
 * - Khi back, camera và trạng thái Map được khôi phục mượt mà.
 */
@Composable
fun HomeContainerScreen(
    homeContainerViewModel: HomeContainerViewModel,
    rootNavController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val uiState by homeContainerViewModel.uiState.collectAsStateWithLifecycle()
    val mapViewportState = rememberMapViewportState()
    val sheetNavController = rememberNavController()
    val bottomSheetState = rememberCustomBottomSheetState()

    // Focus vị trí người dùng 1 lần duy nhất khi mở màn hình lần đầu
    var hasFocusedInitialLocation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.currentLocation) {
        val location = uiState.currentLocation
        if (location != null && !hasFocusedInitialLocation) {
            hasFocusedInitialLocation = true
            focusUserLocationBoundingBox(
                mapViewportState = mapViewportState,
                location = location,
                zoom = 14.5,
                animate = true,
            )
        }
    }

    CommonScreenBottomSheet(
        initState = bottomSheetState,
        backgroundContent = { mapTransform ->
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Mapbox dùng chung — Single Instance
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = mapTransform.scale
                            scaleY = mapTransform.scale
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                ) {
                    TransitStopsMapScreen(
                        modifier = Modifier.fillMaxSize(),
                        transitSources = uiState.transitSources,
                        transitConfigs = uiState.transitConfigs,
                        mapViewportState = mapViewportState,
                        showUserLocation = true,
                        onLocationUpdate = homeContainerViewModel::onLocationUpdated,
                        onStopClick = homeContainerViewModel::onStopClicked,
                        onMapClick = { homeContainerViewModel.onMapClick() },
                        onViewportChanged = homeContainerViewModel::onViewportChanged,
                        onCameraGestureRelease = homeContainerViewModel::onCameraGestureRelease,
                    )
                }

                // 2. Top-Left Floating Settings Button
                Surface(
                    onClick = { /* TODO: Navigate to Settings */ },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 44.dp, start = 16.dp)
                        .size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF333333),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 3. Floating "My Location" Button
                uiState.currentLocation?.let { location ->
                    FloatingActionButton(
                        onClick = {
                            focusUserLocationBoundingBox(
                                mapViewportState = mapViewportState,
                                location = location,
                                zoom = 15.0,
                                animate = true,
                            )
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                bottom = with(bottomSheetState) {
                                    val heightDp = sheetHeightPx / 2.625f // ~dp approximation
                                    (heightDp / 1f).dp.coerceAtLeast(260.dp) + 16.dp
                                },
                                end = 16.dp
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "My Location",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 4. Loading indicator khi đang tải dữ liệu transit
                if (uiState.isLoadingTransit) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Đang tải trạm dừng...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 5. Floating Selected Stop Info Card
                uiState.selectedStop?.let { stop ->
                    StopDetailFloatingCard(
                        stop = stop,
                        onClose = homeContainerViewModel::dismissStopDetail,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(
                                bottom = with(bottomSheetState) {
                                    val heightDp = sheetHeightPx / 2.625f
                                    (heightDp / 1f).dp.coerceAtLeast(260.dp) + 16.dp
                                }, start = 16.dp, end = 16.dp
                            )
                    )
                }
            }
        },
        sheetContent = {
            // NESTED NAVHOST: Nội dung BottomSheet thay đổi theo Navigation
            SheetNavHost(
                sheetNavController = sheetNavController,
                homeContainerViewModel = homeContainerViewModel,
                uiState = uiState,
            )
        }
    )
}
