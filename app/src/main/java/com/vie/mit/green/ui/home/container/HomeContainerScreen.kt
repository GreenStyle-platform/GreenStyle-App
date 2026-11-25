package com.vie.mit.green.ui.home.container

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.vie.mit.common.screenbottomsheet.BottomSheetConfig
import com.vie.mit.common.screenbottomsheet.CommonScreenBottomSheet
import com.vie.mit.common.screenbottomsheet.CustomBottomSheetValue
import com.vie.mit.common.screenbottomsheet.rememberCustomBottomSheetState
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.green.navigation.main_graph.HomeSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbyBikeStationsSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbyBusStopsSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbySubwayStationsSheetRoute
import com.vie.mit.green.navigation.main_graph.NearbyTransitStopsSheetRoute
import com.vie.mit.green.ui.home.SheetNavHost
import com.vie.mit.green.ui.home.homesheet.StopDetailFloatingCard
import com.vie.mit.green.ui.home.nearbysubwaystations.SubwaySheetTab
import com.vie.mit.mapbox.composable.TransitStopsMapScreen
import com.vie.mit.mapbox.util.focusUserLocationBoundingBox
import kotlinx.coroutines.launch
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
    val savedViewport = remember(homeContainerViewModel) {
        homeContainerViewModel.getSavedCameraViewport()
    }
    val mapViewportState = rememberMapViewportState {
        if (savedViewport != null) {
            setCameraOptions {
                center(Point.fromLngLat(savedViewport.centerLongitude, savedViewport.centerLatitude))
                zoom(savedViewport.zoom)
                pitch(0.0)
            }
        } else {
            val fallbackLocation = homeContainerViewModel.getLastKnownLocation()
            if (fallbackLocation != null) {
                setCameraOptions {
                    center(Point.fromLngLat(fallbackLocation.longitude, fallbackLocation.latitude))
                    zoom(14.5)
                    pitch(0.0)
                }
            }
        }
    }
    val sheetNavController = rememberNavController()
    val bottomSheetState = rememberCustomBottomSheetState()

    // Cấu hình động BottomSheetConfig cho từng Sheet route bên trong SheetNavHost
    val currentBackStackEntry by sheetNavController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    var activeSubwayTab by rememberSaveable { mutableStateOf(SubwaySheetTab.STATIONS) }

    // Dismiss selected stop floating card on system Back when staying on HomeSheetRoute
    BackHandler(enabled = uiState.selectedStop != null) {
        homeContainerViewModel.dismissStopDetail()
    }

    val sheetConfig = remember(currentDestination) {
        when {
            currentDestination?.hasRoute<HomeSheetRoute>() == true -> BottomSheetConfig(
                peekedRatio = 0.20f,
                partiallyExpandedRatio = 0.65f,
                expandedRatio = 0.90f,
            )
            currentDestination?.hasRoute<NearbyTransitStopsSheetRoute>() == true ||
                currentDestination?.hasRoute<NearbyBusStopsSheetRoute>() == true ||
                currentDestination?.hasRoute<NearbySubwayStationsSheetRoute>() == true ||
                currentDestination?.hasRoute<NearbyBikeStationsSheetRoute>() == true -> BottomSheetConfig(
                peekedRatio = 0.20f,
                partiallyExpandedRatio = 0.65f,
                expandedRatio = 1f,
            )
            else -> BottomSheetConfig(
                peekedRatio = 0.20f,
                partiallyExpandedRatio = 0.65f,
                expandedRatio = 0.90f,
            )
        }
    }
    val isNearbySheetActive = remember(currentDestination) {
        currentDestination?.hasRoute<NearbyTransitStopsSheetRoute>() == true ||
            currentDestination?.hasRoute<NearbyBusStopsSheetRoute>() == true ||
            currentDestination?.hasRoute<NearbySubwayStationsSheetRoute>() == true ||
            currentDestination?.hasRoute<NearbyBikeStationsSheetRoute>() == true
    }
    val showRadiusCircle = remember(currentDestination, isNearbySheetActive, activeSubwayTab) {
        isNearbySheetActive && !(
            currentDestination?.hasRoute<NearbySubwayStationsSheetRoute>() == true &&
                activeSubwayTab == SubwaySheetTab.ROUTES
            )
    }
    val radiusCircleColor = when {
        currentDestination?.hasRoute<NearbyBusStopsSheetRoute>() == true -> AppTheme.colors.busColor
        currentDestination?.hasRoute<NearbySubwayStationsSheetRoute>() == true -> AppTheme.colors.metroColor
        currentDestination?.hasRoute<NearbyBikeStationsSheetRoute>() == true -> AppTheme.colors.bikeColor
        uiState.selectedTransitTypeId == TransitMode.RAIL.name -> AppTheme.colors.railColor
        else -> AppTheme.colors.navyBlue
    }

    // Single Source of Truth đồng bộ active sheet & transit type filter với HomeContainerViewModel
    LaunchedEffect(currentBackStackEntry) {
        val entry = currentBackStackEntry ?: return@LaunchedEffect
        val destination = entry.destination
        when {
            destination.hasRoute<NearbyBusStopsSheetRoute>() -> {
                homeContainerViewModel.onActiveSheetChanged(
                    isNearbySheet = true,
                    transitTypeId = TransitMode.BUS.name,
                )
            }
            destination.hasRoute<NearbySubwayStationsSheetRoute>() -> {
                homeContainerViewModel.onActiveSheetChanged(
                    isNearbySheet = true,
                    transitTypeId = TransitMode.SUBWAY.name,
                )
            }
            destination.hasRoute<NearbyBikeStationsSheetRoute>() -> {
                homeContainerViewModel.onActiveSheetChanged(
                    isNearbySheet = true,
                    transitTypeId = TravelMode.BICYCLE_RENT.name,
                )
            }
            destination.hasRoute<NearbyTransitStopsSheetRoute>() -> {
                val route = runCatching { entry.toRoute<NearbyTransitStopsSheetRoute>() }.getOrNull()
                homeContainerViewModel.onActiveSheetChanged(
                    isNearbySheet = true,
                    transitTypeId = route?.transitTypeId,
                )
            }
            else -> {
                activeSubwayTab = SubwaySheetTab.STATIONS
                homeContainerViewModel.onActiveSheetChanged(
                    isNearbySheet = false,
                    transitTypeId = null,
                )
            }
        }
    }

    // Map lưu trữ trạng thái hiển thị của từng Route trong SheetNavHost
    val savedSheetStates = remember { mutableMapOf<String, CustomBottomSheetValue>() }
    var previousRouteKey by remember { mutableStateOf<String?>(null) }

    // Quản lý lưu và khôi phục trạng thái BottomSheet khi chuyển Sheet Route
    LaunchedEffect(currentBackStackEntry) {
        val entry = currentBackStackEntry ?: return@LaunchedEffect
        val currentRouteKey = entry.id

        // 1. Lưu lại trạng thái của route trước đó trước khi đổi route
        val prevRoute = previousRouteKey
        if (prevRoute != null && prevRoute != currentRouteKey) {
            savedSheetStates[prevRoute] = bottomSheetState.currentValue
        }
        previousRouteKey = currentRouteKey

        // 2. Bỏ qua animate thừa ở lần khởi tạo đầu tiên khi sheet đã ở đúng PartiallyExpanded
        if (prevRoute == null) return@LaunchedEffect

        // 3. Khôi phục trạng thái đã lưu hoặc mở PartiallyExpanded mặc định cho route mới
        val savedValue = savedSheetStates[currentRouteKey]
        if (savedValue != null) {
            bottomSheetState.animateTo(savedValue)
        } else {
            bottomSheetState.partialExpand()
        }
    }

    // Chiều cao padding đáy của Mapbox Camera tương ứng với chiều cao của BottomSheet.
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

    // Alpha mượt mà cho các floating overlay phía trên sheet: mờ dần khi kéo sheet mở rộng quá PartiallyExpanded
    val floatingOverlayAlpha by remember(bottomSheetState) {
        derivedStateOf {
            (1f - bottomSheetState.expandProgress * 2f).coerceIn(0f, 1f)
        }
    }

    // One-shot camera focus events (lần đầu định vị GPS, nút My Location, hoặc chọn trạm không có detail screen)
    LaunchedEffect(homeContainerViewModel) {
        homeContainerViewModel.cameraFocusEvents.collect { command ->
            if (command.collapseExpandedSheet && bottomSheetState.targetValue == CustomBottomSheetValue.Expanded) {
                launch {
                    bottomSheetState.partialExpand()
                }
            }
            focusUserLocationBoundingBox(
                mapViewportState = mapViewportState,
                location = command.location,
                zoom = command.zoom,
                animate = command.animate,
                bottomPaddingPx = cameraBottomPadding.toDouble(),
            )
        }
    }

    // Memoize callback click vùng trống trên map: đóng card trạm đang chọn nếu có, ngược lại mở NearbyTransitStopsSheet từ HomeSheet
    val onMapClick = remember(sheetNavController, currentDestination, uiState.selectedStop) {
        { _: Point ->
            if (uiState.selectedStop != null) {
                homeContainerViewModel.dismissStopDetail()
            } else if (currentDestination?.hasRoute<HomeSheetRoute>() == true) {
                sheetNavController.navigate(NearbyTransitStopsSheetRoute()) {
                    launchSingleTop = true
                }
            }
        }
    }

    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    CommonScreenBottomSheet(
        modifier = modifier,
        initState = bottomSheetState,
        config = sheetConfig,
        backgroundContent = { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Mapbox dùng chung — Single Instance (Camera Padding)
                TransitStopsMapScreen(
                    modifier = Modifier.fillMaxSize(),
                    transitSources = uiState.transitSources,
                    transitConfigs = uiState.transitConfigs,
                    mapViewportState = mapViewportState,
                    showUserLocation = true,
                    showRadiusCircle = showRadiusCircle,
                    radiusCircleColor = radiusCircleColor,
                    currentRadiusKm = uiState.selectedRadiusKm,
                    cameraBottomPadding = cameraBottomPadding,
                    maxZoom = 18.0,
                    minZoom = 10.0,
                    onLocationUpdate = homeContainerViewModel::onLocationUpdated,
                    onStopClick = homeContainerViewModel::onStopClicked,
                    onMapClick = onMapClick,
                    onViewportChanged = homeContainerViewModel::onViewportChanged,
                    onCameraGestureRelease = homeContainerViewModel::onCameraGestureRelease,
                    onRadiusCalculated = homeContainerViewModel::setSelectedRadiusKm,
                )

                // 2. Top-Left Floating Settings Button
                Surface(
                    onClick = homeContainerViewModel::navigateToSettingScreen,
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 12.dp, start = 16.dp)
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

                // 3. Loading indicator khi đang tải dữ liệu transit
                if (uiState.isLoadingTransit) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 12.dp)
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

                // 4. Stacked Bottom Floating Controls (My Location FAB + Selected Stop Card)
                // Neo theo cameraBottomPadding (giới hạn tại PartiallyExpanded) và mờ dần khi sheet kéo lên Expanded
                if (floatingOverlayAlpha > 0.01f) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .offset {
                                IntOffset(0, -cameraBottomPadding.roundToInt())
                            }
                            .graphicsLayer { alpha = floatingOverlayAlpha },
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FloatingActionButton(
                            onClick = {
                                val latestLocation =
                                    homeContainerViewModel.getLastKnownLocation() ?: uiState.currentLocation
                                if (latestLocation != null) {
                                    homeContainerViewModel.onUserLocationButtonClicked(latestLocation)
                                }
                            },
                            shape = CircleShape,
                            containerColor = AppTheme.colors.white,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "My Location",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        uiState.selectedStop?.let { stop ->
                            StopDetailFloatingCard(
                                stop = stop,
                                onClose = homeContainerViewModel::dismissStopDetail,
                                onViewDetail = if (
                                    homeContainerViewModel.canNavigateToStopDetail(stop.typeId) &&
                                    stop.stopId.isNotBlank()
                                ) {
                                    { homeContainerViewModel.navigateToSelectedStopDetail(stop) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        },
        sheetContent = {
            // Thêm top padding linh hoạt theo status bar khi sheet giãn lên full màn hình (expandedRatio == 1f)
            val dynamicTopPadding = if (sheetConfig.expandedRatio >= 0.95f) {
                statusBarTopPadding * bottomSheetState.expandProgress
            } else {
                0.dp
            }
            // NESTED NAVHOST: Nội dung BottomSheet thay đổi theo Navigation
            SheetNavHost(
                homeNavController = sheetNavController,
                homeContainerViewModel = homeContainerViewModel,
                onSubwayTabChanged = { activeSubwayTab = it },
                modifier = Modifier.padding(top = dynamicTopPadding),
            )
        }
    )
}
