package com.vie.mit.green.ui.routeplanner.container

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
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
import com.vie.mit.common.theme.AppColors
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.common.ui.button.FloatingBackButton
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.green.R
import com.vie.mit.green.navigation.main_graph.RouteLocationPickerSheetRoute
import com.vie.mit.green.navigation.main_graph.RouteOptionsRouteSheet
import com.vie.mit.mapbox.composable.RoutePickerMap
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.RouteMapSegment
import com.vie.mit.mapbox.model.RouteMapStopPoint
import com.vie.mit.mapbox.util.focusUserLocationBoundingBox
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * RoutePlannerContainerScreen: Container chính cho toàn bộ luồng tìm kiếm và hiển thị lộ trình (Citymapper style).
 * - backgroundContent: Giữ duy nhất 1 Mapbox instance làm nền, đồng bộ cameraBottomPadding theo BottomSheet.
 * - sheetContent: Chứa Nested NavHost ([RoutePlannerNavHost]) chuyển đổi mượt mà giữa:
 *     1. [RouteLocationPickerSheetRoute]: Peeked = 0.25f, PartiallyExpanded = 0.86f, Expanded = 1.0f.
 *     2. [RouteOptionsRouteSheet]: Peeked = 0.25f, PartiallyExpanded = 0.60f, Expanded = 1.0f.
 */
@Composable
fun RoutePlannerContainerScreen(
    containerViewModel: RoutePlannerContainerViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by containerViewModel.uiState.collectAsStateWithLifecycle()
    val routePlannerNavController = rememberNavController()
    val bottomSheetState = rememberCustomBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(106.7009, 10.7769))
            zoom(14.0)
        }
    }

    // Kiểm tra sheet hiện tại đang hiển thị
    val currentBackStackEntry by routePlannerNavController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val isLocationPickerActive = remember(currentDestination) {
        currentDestination?.hasRoute<RouteOptionsRouteSheet>() != true
    }

    // Cấu hình BottomSheet động theo từng Sheet route (tương tự HomeContainerScreen)
    val sheetConfig = remember(currentDestination) {
        when {
            currentDestination?.hasRoute<RouteOptionsRouteSheet>() == true -> BottomSheetConfig(
                peekedRatio = 0.25f,
                partiallyExpandedRatio = 0.60f,
                expandedRatio = 1.0f,
            )
            currentDestination?.hasRoute<RouteLocationPickerSheetRoute>() == true -> BottomSheetConfig(
                peekedRatio = 0.25f,
                partiallyExpandedRatio = 0.86f,
                expandedRatio = 1.0f,
            )
            else -> BottomSheetConfig(
                peekedRatio = 0.25f,
                partiallyExpandedRatio = 0.86f,
                expandedRatio = 1.0f,
            )
        }
    }

    // Lưu và khôi phục trạng thái BottomSheet khi chuyển đổi giữa Picker và Options (giống HomeContainerScreen)
    val savedSheetStates = remember { mutableMapOf<String, CustomBottomSheetValue>() }
    var previousRouteKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentBackStackEntry) {
        val entry = currentBackStackEntry ?: return@LaunchedEffect
        val currentRouteKey = entry.id

        val prevRoute = previousRouteKey
        if (prevRoute != null && prevRoute != currentRouteKey) {
            savedSheetStates[prevRoute] = bottomSheetState.currentValue
        }
        previousRouteKey = currentRouteKey

        if (prevRoute == null) return@LaunchedEffect

        if (entry.destination.hasRoute<RouteOptionsRouteSheet>()) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            bottomSheetState.partialExpand()
        } else {
            val savedValue = savedSheetStates[currentRouteKey]
            if (savedValue != null) {
                bottomSheetState.animateTo(savedValue)
            } else {
                bottomSheetState.partialExpand()
            }
        }
    }

    // Đồng bộ Bàn phím (IME) -> BottomSheet:
    // Khi bàn phím vừa mở lên -> đẩy BottomSheet sang state PartiallyExpanded (0.86f)
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    var wasImeVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isImeVisible, isLocationPickerActive) {
        val imeJustOpened = isImeVisible && !wasImeVisible
        wasImeVisible = isImeVisible
        if (
            imeJustOpened &&
            isLocationPickerActive &&
            bottomSheetState.targetValue != CustomBottomSheetValue.PartiallyExpanded
        ) {
            bottomSheetState.partialExpand()
        }
    }

    // Đảm bảo ẩn bàn phím & clear focus khi rời màn hình (kể cả nhấn Back hệ thống / Predictive Back)
    DisposableEffect(keyboardController, focusManager) {
        onDispose {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    // Đồng bộ BottomSheet -> Bàn phím:
    // Khi người dùng kéo BottomSheet xuống Peeked (hoặc hạ xuống Peeked) -> ẩn bàn phím & clear focus
    LaunchedEffect(bottomSheetState) {
        snapshotFlow { bottomSheetState.targetValue }
            .distinctUntilChanged()
            .collect { target ->
                if (target == CustomBottomSheetValue.Peeked) {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                }
            }
    }

    // Đồng bộ camera padding theo chiều cao thực tế của sheet (giống HomeContainerScreen)
    // Khi BottomSheet ở anchor PartiallyExpanded và kéo lên (Expanded), padding đáy giữ nguyên không đổi.
    // Khi BottomSheet hạ xuống (về Peeked), cameraBottomPadding giảm mượt mà theo sheetHeightPx.
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

    // Alpha mượt mà cho các controls nổi trên map:
    // - Khi ở RouteLocationPickerSheet (partial = 0.86f): hiển thị rõ khi sheet hạ gần về Peeked (0.25f)
    // - Khi ở RouteOptionsSheet (partial = 0.60f): hiển thị rõ ở Peeked & PartiallyExpanded, mờ dần khi lên Expanded
    val mapControlsAlpha by remember(bottomSheetState, isLocationPickerActive) {
        derivedStateOf {
            if (isLocationPickerActive) {
                (bottomSheetState.dragDownFromPartialProgress * 1.5f).coerceIn(0f, 1f)
            } else {
                (1f - bottomSheetState.expandProgress * 2f).coerceIn(0f, 1f)
            }
        }
    }

    // Lắng nghe sự kiện di chuyển camera từ Container ViewModel
    LaunchedEffect(containerViewModel) {
        containerViewModel.cameraMoveEvent.collect { target ->
            focusUserLocationBoundingBox(
                mapViewportState = mapViewportState,
                location = target,
                zoom = 15.0,
                animate = true,
                bottomPaddingPx = cameraBottomPadding.toDouble(),
            )
        }
    }

    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val collapsePickerToPeeked: () -> Unit = remember(bottomSheetState, coroutineScope, keyboardController, focusManager) {
        {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            if (bottomSheetState.targetValue != CustomBottomSheetValue.Peeked) {
                coroutineScope.launch {
                    bottomSheetState.peek()
                }
            }
        }
    }

    val expandPickerFromPeeked: () -> Unit = remember(bottomSheetState, coroutineScope) {
        {
            if (
                bottomSheetState.currentValue == CustomBottomSheetValue.Peeked ||
                bottomSheetState.targetValue == CustomBottomSheetValue.Peeked
            ) {
                coroutineScope.launch {
                    bottomSheetState.partialExpand()
                }
            }
        }
    }

    val colors = AppTheme.colors
    val routeFitBottomRatio by remember(bottomSheetState, sheetConfig, isLocationPickerActive) {
        derivedStateOf {
            if (isLocationPickerActive) {
                0f
            } else {
                when (bottomSheetState.targetValue) {
                    CustomBottomSheetValue.Peeked -> sheetConfig.peekedRatio
                    CustomBottomSheetValue.Hidden -> 0f
                    CustomBottomSheetValue.PartiallyExpanded,
                    CustomBottomSheetValue.Expanded,
                    -> sheetConfig.partiallyExpandedRatio
                }
            }
        }
    }
    val selectedRouteOption = if (!isLocationPickerActive) uiState.selectedRouteOption else null
    val routeMapSegments = remember(selectedRouteOption, colors) {
        selectedRouteOption?.legs.orEmpty().mapNotNull { leg ->
            if (leg.points.size < 2) return@mapNotNull null
            RouteMapSegment(
                points = leg.points.map { LocationData(latitude = it.latitude, longitude = it.longitude) },
                colorHex = leg.mode.toRouteColor(colors).toHexRgb(),
                isDashed = leg.mode == TransitMode.WALK,
            )
        }
    }
    val transferStops = remember(selectedRouteOption, colors) {
        selectedRouteOption?.legs.orEmpty()
            .filter { it.mode.isTransit }
            .flatMap { leg ->
                val colorHex = leg.mode.toRouteColor(colors).toHexRgb()
                listOfNotNull(leg.from, leg.to).map { place ->
                    RouteMapStopPoint(
                        name = place.name,
                        location = LocationData(latitude = place.latitude, longitude = place.longitude),
                        colorHex = colorHex,
                    )
                }
            }
    }

    CommonScreenBottomSheet(
        initState = bottomSheetState,
        config = sheetConfig,
        backgroundContent = { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Bản đồ Mapbox nền (Single Instance)
                RoutePickerMap(
                    startPoint = uiState.startPoint,
                    endPoint = uiState.endPoint,
                    routeSegments = routeMapSegments,
                    transferStops = transferStops,
                    onCameraCenterChanged = { lat, lng ->
                        if (isLocationPickerActive) {
                            containerViewModel.onMapCameraMoved(lat, lng)
                        }
                    },
                    cameraBottomPadding = cameraBottomPadding,
                    routeFitBottomRatio = routeFitBottomRatio,
                    fitRouteBounds = !isLocationPickerActive,
                    mapViewportState = mapViewportState,
                    onLocationUpdate = containerViewModel::onLocationUpdated,
                    onMapClick = {
                        if (isLocationPickerActive) {
                            collapsePickerToPeeked()
                        }
                    },
                    onMapMoveStateChanged = { isMoving ->
                        if (isLocationPickerActive) {
                            containerViewModel.onMapMoveStateChanged(isMoving)
                            if (isMoving && bottomSheetState.targetValue != CustomBottomSheetValue.Peeked) {
                                collapsePickerToPeeked()
                            }
                        }
                    },
                    onCameraIdle = { lat, lng ->
                        if (isLocationPickerActive) {
                            containerViewModel.onMapCameraIdle(lat, lng)
                        }
                    },
                    centerOverlay = {
                        // Luôn giữ icon Start / End tại tâm vùng bản đồ hiển thị khi đang chọn vị trí
                        if (isLocationPickerActive && mapControlsAlpha > 0.01f) {
                            CenterLocationMarker(
                                isStart = uiState.focusedField == LocationPickerField.START,
                                isMapMoving = uiState.isMapMoving,
                                modifier = Modifier.graphicsLayer { alpha = mapControlsAlpha },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // 2. Nút Back tròn nổi góc trên bên trái
                FloatingBackButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
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

                // 3. Floating Controls neo theo mép trên BottomSheet (My Location + Xác nhận tâm bản đồ)
                if (mapControlsAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .offset {
                                IntOffset(0, -cameraBottomPadding.roundToInt())
                            }
                            .graphicsLayer { alpha = mapControlsAlpha },
                    ) {
                        // Nút "My Location" góc dưới phải của vùng map
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
                                        bottomPaddingPx = cameraBottomPadding.toDouble(),
                                    )
                                }
                            },
                            shape = CircleShape,
                            containerColor = AppTheme.colors.white,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Vị trí của tôi",
                                modifier = Modifier.size(22.dp),
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
            RoutePlannerNavHost(
                routePlannerNavController = routePlannerNavController,
                routePlannerContainerViewModel = containerViewModel,
                onSearchFieldClicked = expandPickerFromPeeked,
                onPickOnMapClicked = collapsePickerToPeeked,
                onNavigateToRouteOptions = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    containerViewModel.loadRouteOptions()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = dynamicTopPadding),
            )
        },
        modifier = modifier.fillMaxSize(),
    )
}

private val CenterMarkerSize = 32.dp

/**
 * Center Marker nằm chính giữa viewport bản đồ:
 * - Hiển thị Icon Start (Xanh lá) khi đang focus ô Start.
 * - Hiển thị Icon End (Đỏ) khi đang focus ô End.
 * - Dùng bottom padding bằng chính [CenterMarkerSize] thay vì negative offset để giữ toàn bộ icon
 *   nằm trong layout bounds (tránh bị offscreen buffer của `graphicsLayer { alpha }` cắt mất nửa trên
 *   khi 0f < alpha < 1f), đồng thời vẫn đặt mũi nhọn đáy pin đúng tại tâm bản đồ.
 */
@Composable
private fun CenterLocationMarker(
    isStart: Boolean,
    isMapMoving: Boolean,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(if (isStart) R.drawable.ic_start_location else R.drawable.ic_end_location),
        contentDescription = "pickLocation",
        alpha = if (isMapMoving) 0.6f else 1f,
        modifier = modifier
            .padding(bottom = CenterMarkerSize)
            .size(CenterMarkerSize),
    )
}

internal fun TransitMode.toRouteColor(colors: AppColors): Color = when (this) {
    TransitMode.BUS, TransitMode.TROLLEYBUS -> colors.busColor
    TransitMode.SUBWAY, TransitMode.MONORAIL -> colors.metroColor
    TransitMode.RAIL, TransitMode.TRAM, TransitMode.CABLE_TRAM, TransitMode.AERIAL_LIFT -> colors.railColor
    TransitMode.BIKE -> colors.bikeColor
    TransitMode.FERRY -> colors.navyBlue
    TransitMode.WALK -> colors.secondaryText
}

private fun Color.toHexRgb(): String = String.format(Locale.US, "#%06X", toArgb() and 0xFFFFFF)
