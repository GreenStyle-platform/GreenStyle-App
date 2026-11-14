package com.vie.mit.green.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.vie.mit.common.screenbottomsheet.BottomSheetConfig
import com.vie.mit.common.screenbottomsheet.CommonScreenBottomSheet
import com.vie.mit.common.screenbottomsheet.CustomBottomSheetValue
import com.vie.mit.common.screenbottomsheet.rememberCustomBottomSheetState
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.green.navigation.main_graph.HomeSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbyTransitStopsSheetRoute
import com.vie.mit.green.navigation.main_graph.StopDetailSheetRoute
import com.vie.mit.green.ui.home.homesheet.StopDetailFloatingCard
import com.vie.mit.mapbox.composable.TransitStopsMapScreen
import com.vie.mit.mapbox.util.focusUserLocationBoundingBox
import kotlin.math.roundToInt

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
    modifier: Modifier = Modifier,
) {
    val uiState by homeContainerViewModel.uiState.collectAsStateWithLifecycle()
    val mapViewportState = rememberMapViewportState()
    val sheetNavController = rememberNavController()
    val bottomSheetState = rememberCustomBottomSheetState()

    // Cấu hình động BottomSheetConfig cho từng Sheet route bên trong SheetNavHost
    val currentBackStackEntry by sheetNavController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val sheetConfig = remember(currentDestination) {
        when {
            currentDestination?.hasRoute<HomeSheetRoute>() == true -> BottomSheetConfig(
                peekedRatio = 0.20f,
                partiallyExpandedRatio = 0.65f,
                expandedRatio = 0.90f,
            )
            currentDestination?.hasRoute<NearbyTransitStopsSheetRoute>() == true -> BottomSheetConfig(
                peekedRatio = 0.20f,
                partiallyExpandedRatio = 0.65f,
                expandedRatio = 0.90f,
            )
            currentDestination?.hasRoute<StopDetailSheetRoute>() == true -> BottomSheetConfig(
                peekedRatio = 0.20f,
                partiallyExpandedRatio = 0.45f,
                expandedRatio = 0.85f,
            )
            else -> BottomSheetConfig()
        }
    }

    // Map lưu trữ trạng thái hiển thị của từng Route trong SheetNavHost
    val savedSheetStates = remember { mutableMapOf<String, CustomBottomSheetValue>() }
    var previousRouteKey by remember { mutableStateOf<String?>(null) }

    // Quản lý lưu và khôi phục trạng thái BottomSheet khi chuyển Sheet Route
    LaunchedEffect(currentBackStackEntry) {
        val entry = currentBackStackEntry ?: return@LaunchedEffect
        val currentRouteKey = entry.destination.route ?: entry.destination.id.toString()

        // 1. Lưu lại trạng thái của route trước đó trước khi đổi route
        previousRouteKey?.let { prevRoute ->
            if (prevRoute != currentRouteKey) {
                savedSheetStates[prevRoute] = bottomSheetState.currentValue
            }
        }
        previousRouteKey = currentRouteKey

        // 2. Dọn dẹp cache của các destination đã bị pop khỏi back stack
        @Suppress("RestrictedApi")
        val activeRouteKeys: Set<String> = sheetNavController.currentBackStack.value
            .mapNotNull { it.destination.route ?: it.destination.id.toString() }
            .toSet()
        savedSheetStates.keys.retainAll(activeRouteKeys)

        // 3. Khôi phục trạng thái đã lưu hoặc mở PartiallyExpanded mặc định cho route mới
        val savedValue = savedSheetStates[currentRouteKey]
        if (savedValue != null) {
            bottomSheetState.animateTo(savedValue)
        } else {
            bottomSheetState.partialExpand()
        }
    }

    // Phương pháp 2: Chiều cao padding đáy của Mapbox Camera tương ứng với chiều cao của BottomSheet.
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

    LaunchedEffect(uiState.currentLocation) {
        val location = uiState.currentLocation
        if (location != null ) {
            focusUserLocationBoundingBox(
                mapViewportState = mapViewportState,
                location = location,
                zoom = 14.5,
                animate = true,
            )
        }
    }

    // Khi người dùng chạm bản đồ (isMapFocused), hạ sheet xuống trạng thái Peeked để xem bản đồ
    LaunchedEffect(uiState.isMapFocused) {
//        if (uiState.isMapFocused) {
//            bottomSheetState.peek()
//        }
    }

    // Memoize callback click trên map để tránh tạo lại đối tượng lambda ở mỗi recomposition
    val onMapClick = remember(sheetNavController, currentDestination, homeContainerViewModel) {
        { point: Point ->
            if (currentDestination?.hasRoute<HomeSheetRoute>() == true) {
                sheetNavController.navigate(NearbyTransitStopsSheetRoute)
            }
        }
    }

    CommonScreenBottomSheet(
        modifier = modifier,
        initState = bottomSheetState,
        config = sheetConfig,
        backgroundContent = { mapTransform ->
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Mapbox dùng chung — Single Instance (Phương pháp 2: Camera Padding)
                TransitStopsMapScreen(
                    modifier = Modifier.fillMaxSize(),
                    transitSources = uiState.transitSources,
                    transitConfigs = uiState.transitConfigs,
                    mapViewportState = mapViewportState,
                    showUserLocation = true,
                    cameraBottomPadding = cameraBottomPadding,
                    maxZoom = 18.0,
                    minZoom = 10.0 ,
                    onLocationUpdate = homeContainerViewModel::onLocationUpdated,
                    onStopClick = homeContainerViewModel::onStopClicked,
                    onMapClick = onMapClick,
                    onViewportChanged = homeContainerViewModel::onViewportChanged,
                    onCameraGestureRelease = homeContainerViewModel::onCameraGestureRelease,
                )

                // 2. Top-Left Floating Settings Button
                Surface(
                    onClick = { /* TODO: Navigate to Settings */ },
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AppTheme.colors.black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 3. Floating "My Location" Button (di chuyển mượt theo mép sheet không recompose)
                FloatingActionButton(
                    onClick = {
                        val latestLocation = homeContainerViewModel.getLastKnownLocation() ?: uiState.currentLocation
                        if (latestLocation != null) {
                            homeContainerViewModel.onUserLocationButtonClicked(latestLocation)
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
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 4. Loading indicator khi đang tải dữ liệu transit
                if (uiState.isLoadingTransit) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp)
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
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .offset {
                                IntOffset(0, -bottomSheetState.sheetHeightPx.roundToInt())
                            }
                    )
                }
            }
        },
        sheetContent = {
            // NESTED NAVHOST: Nội dung BottomSheet thay đổi theo Navigation
            SheetNavHost(
                sheetNavController = sheetNavController,
                homeContainerViewModel = homeContainerViewModel,
            )
        }
    )
}
