package com.vie.mit.green.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.vie.mit.green.ui.component.bottomsheet.CommonBottomSheet
import com.vie.mit.mapbox.composable.TransitStopsMapScreen
import com.vie.mit.mapbox.model.StopFeatureClick
import com.vie.mit.mapbox.util.focusUserLocationBoundingBox

// Citymapper Color Palette
private val CitymapperGreen = Color(0xFF1FAF4E)
private val CitymapperNavBlue = Color(0xFF35698F)
private val CitymapperButtonOverlay = Color(0x33000000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mapViewportState = rememberMapViewportState()

    // Chỉ focus vị trí người dùng 1 lần duy nhất khi mở màn hình lần đầu
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

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    CommonBottomSheet(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 240.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = CitymapperGreen,
        sheetContentColor = Color.White,
        sheetContent = {
            when (uiState.activeBottomSheet) {
                HomeBottomSheetType.HOME -> {
                    HomeBottomSheetContent(
                        activeTransitTypes = uiState.activeTransitTypes,
                        onToggleTransitType = viewModel::toggleTransitType,
                    )
                }
                HomeBottomSheetType.NEARBY_STOPS -> {
                    NearbyStopsBottomSheetContent(
                        onBackToHome = viewModel::showHomeBottomSheet,
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Pure Mapbox transit stops map with clustering (Free camera panning)
            TransitStopsMapScreen(
                modifier = Modifier.fillMaxSize(),
                transitSources = uiState.transitSources,
                transitConfigs = uiState.transitConfigs,
                mapViewportState = mapViewportState,
                showUserLocation = true,
                onLocationUpdate = viewModel::onLocationUpdated,
                onStopClick = viewModel::onStopClicked,
                onMapClick = { viewModel.onMapClick() },
                onViewportChanged = viewModel::onViewportChanged,
                onCameraGestureRelease = viewModel::onCameraGestureRelease,
            )

            // 2. Top-Left Floating Settings Button
            Surface(
                onClick = { /* TODO: Open Settings */ },
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

            // 3. Floating "My Location" Button (để người dùng chủ động quay lại vị trí của mình khi cần)
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
                        .padding(bottom = 24.dp, end = 16.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 4. Loading indicator when fetching transit data
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

            // 5. Floating Selected Stop Info Card (if selected)
            uiState.selectedStop?.let { stop ->
                StopDetailFloatingCard(
                    stop = stop,
                    onClose = viewModel::dismissStopDetail,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeBottomSheetContent(
    activeTransitTypes: Set<String>,
    onToggleTransitType: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onWorkClick: () -> Unit = {},
    onPlacesClick: () -> Unit = {},
    onTicketClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. White Navigation Card ("Get Me Somewhere", "Get Me Home", "Work", "Places")
        CitymapperNavigationCard(
            onSearchClick = onSearchClick,
            onHomeClick = onHomeClick,
            onWorkClick = onWorkClick,
            onPlacesClick = onPlacesClick,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Thành phần 2: Phương tiện công cộng (Transit Modes Grid 2x5)
        Text(
            text = "Phương tiện công cộng",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        TransitModesGrid(
            activeTransitTypes = activeTransitTypes,
            onToggleTransitType = onToggleTransitType,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Thành phần 3: Tiện ích (Vé, Lịch sử chuyến đi)
        Text(
            text = "Tiện ích",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        UtilitiesGrid(
            onTicketClick = onTicketClick,
            onHistoryClick = onHistoryClick,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun NearbyStopsBottomSheetContent(
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header with title and close/back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Trạm dừng xung quanh",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            IconButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .size(32.dp)
                    .background(CitymapperButtonOverlay, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Trở về Trang chủ",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Info card showing location status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = CitymapperNavBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vị trí đã chọn trên bản đồ",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Các trạm dừng xe buýt, metro, tàu hỏa và điểm xe đạp xung quanh khu vực này.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sample nearby stops cards (UI structure for nearby stops)
        NearbyStopItemCard(
            title = "Trạm xe buýt Bến Thành",
            distance = "120m",
            transitType = "Bus",
            icon = Icons.Default.DirectionsBus,
            routes = "Tuyến 01, 02, 03, 150"
        )

        Spacer(modifier = Modifier.height(8.dp))

        NearbyStopItemCard(
            title = "Ga Metro Bến Thành",
            distance = "250m",
            transitType = "Metro",
            icon = Icons.Default.Subway,
            routes = "Tuyến Metro 1 (Bến Thành - Suối Tiên)"
        )

        Spacer(modifier = Modifier.height(8.dp))

        NearbyStopItemCard(
            title = "Trạm xe đạp công cộng Lê Lợi",
            distance = "300m",
            transitType = "Bike",
            icon = Icons.AutoMirrored.Filled.DirectionsBike,
            routes = "12 xe khả dụng"
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun NearbyStopItemCard(
    title: String,
    distance: String,
    transitType: String,
    icon: ImageVector,
    routes: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(CitymapperNavBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transitType,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
                Text(
                    text = routes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5563)
                )
            }
            Text(
                text = distance,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = CitymapperNavBlue
                )
            )
        }
    }
}

@Composable
private fun CitymapperNavigationCard(
    onSearchClick: () -> Unit,
    onHomeClick: () -> Unit,
    onWorkClick: () -> Unit,
    onPlacesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Upper Search Bar: "Get Me Somewhere"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CitymapperNavBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Destination",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Get Me Somewhere",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                    )
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = Color(0xFFEEEEEE)
            )

            // Lower Quick Destinations: Get Me Home | Work | Places
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Get Me Home
                Row(
                    modifier = Modifier
                        .weight(1.3f)
                        .clickable(onClick = onHomeClick)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(CitymapperNavBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Get Me Home",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Get Me Home",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    )
                }

                VerticalDivider(
                    thickness = 1.dp,
                    color = Color(0xFFEEEEEE),
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                )

                // Work
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .clickable(onClick = onWorkClick)
                        .padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(CitymapperNavBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = "Work",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Work",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    )
                }

                VerticalDivider(
                    thickness = 1.dp,
                    color = Color(0xFFEEEEEE),
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                )

                // Places
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .clickable(onClick = onPlacesClick)
                        .padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(CitymapperNavBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Places",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Places",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TransitModesGrid(
    activeTransitTypes: Set<String>,
    onToggleTransitType: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: All | Walk | Bike | Maps | Issues (with badge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CitymapperGridButton(
                icon = Icons.Default.DirectionsTransit,
                label = "All",
                onClick = { /* All mode action */ },
                modifier = Modifier.weight(1f),
            )
            CitymapperGridButton(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                label = "Walk",
                onClick = { /* Walk action */ },
                modifier = Modifier.weight(1f),
            )
            CitymapperGridButton(
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                label = "Bike",
                isActive = activeTransitTypes.contains("BIKE"),
                onClick = { onToggleTransitType("BIKE") },
                modifier = Modifier.weight(1f),
            )
            CitymapperGridButton(
                icon = Icons.Default.Map,
                label = "Maps",
                onClick = { /* Open maps action */ },
                modifier = Modifier.weight(1f),
            )
            CitymapperGridButton(
                icon = Icons.Default.WarningAmber,
                label = "Issues",
                badgeText = "14",
                onClick = { /* Issues action */ },
                modifier = Modifier.weight(1f),
            )
        }

        // Row 2: e-Scooter | Bus | Tube | Rail | More
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CitymapperGridButton(
                icon = Icons.Default.ElectricScooter,
                label = "e-Scooter",
                onClick = { /* e-Scooter action */ },
                modifier = Modifier.weight(1f),
            )
            CitymapperGridButton(
                icon = Icons.Default.DirectionsBus,
                label = "Bus",
                isActive = activeTransitTypes.contains("BUS"),
                onClick = { onToggleTransitType("BUS") },
                modifier = Modifier.weight(1f),
            )
            CitymapperGridButton(
                icon = Icons.Default.Subway,
                label = "Tube",
                isActive = activeTransitTypes.contains("METRO"),
                onClick = { onToggleTransitType("METRO") },
                modifier = Modifier.weight(1f),
            )
            CitymapperGridButton(
                icon = Icons.Default.Train,
                label = "Rail",
                isActive = activeTransitTypes.contains("TRAIN"),
                onClick = { onToggleTransitType("TRAIN") },
                modifier = Modifier.weight(1f),
            )
            CitymapperGridButton(
                icon = Icons.Default.KeyboardArrowDown,
                label = "More",
                onClick = { /* More transit types */ },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun UtilitiesGrid(
    onTicketClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 5 slots in row so items have the exact same size as the items in TransitModesGrid
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CitymapperGridButton(
            icon = Icons.Default.ConfirmationNumber,
            label = "Vé",
            onClick = onTicketClick,
            modifier = Modifier.weight(1f),
        )
        CitymapperGridButton(
            icon = Icons.Default.History,
            label = "Lịch sử chuyến đi",
            onClick = onHistoryClick,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CitymapperGridButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    isActive: Boolean = true,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) CitymapperButtonOverlay else Color(0x18000000),
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(16.dp)
                            .background(Color(0xFFE65100), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StopDetailFloatingCard(
    stop: StopFeatureClick,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsTransit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stop.name ?: "Trạm dừng công cộng",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Loại: ${stop.typeId} | ID: ${stop.stopId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stop extra properties (e.g. routes or available bikes)
            val info = stop.properties["routes"]
                ?: stop.properties["line"]
                ?: stop.properties["available_bikes"]?.let { "Xe khả dụng: $it" }
            if (info != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
