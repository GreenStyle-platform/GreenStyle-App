package com.vie.mit.green.ui.home.homesheet

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.green.R
import com.vie.mit.mapbox.model.StopFeatureClick

// ======================================================================================
// Sheet Content Composables — Dùng bên trong SheetNavHost (Nested NavHost)
// ======================================================================================

/**
 * HomeSheetContent: Nội dung BottomSheet trang chủ.
 * Hiển thị thanh tìm kiếm, lưới phương tiện công cộng, và tiện ích.
 */
@Composable
internal fun HomeSheet(
    activeTransitTypes: Set<String>,
    onToggleTransitType: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onWorkClick: () -> Unit = {},
    onPlacesClick: () -> Unit = {},
    onTicketClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    /**
     * Called when user taps on a transit type tile (single click to navigate to that type's nearby sheet).
     * Separate from [onToggleTransitType] which handles map visibility toggling.
     */
    onTransitTypeClick: (typeId: String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. White Navigation Card ("Get Me Somewhere", "Get Me Home", "Work", "Places")
        NavigationCard(
            onSearchClick = onSearchClick,
            onHomeClick = onHomeClick,
            onWorkClick = onWorkClick,
            onPlacesClick = onPlacesClick,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Phương tiện công cộng (Transit Modes Grid 2x5)
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
            onTransitTypeClick = onTransitTypeClick,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Tiện ích (Vé, Lịch sử chuyến đi)
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

// ======================================================================================
// Floating Card — Dùng bởi MapContainerScreen
// ======================================================================================

@Composable
internal fun StopDetailFloatingCard(
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

// ======================================================================================
// Private UI Components
// ======================================================================================

@Composable
private fun NavigationCard(
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
                        .background(AppTheme.colors.navyBlue, CircleShape),
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
                            .background(AppTheme.colors.navyBlue, CircleShape),
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
                            .background(AppTheme.colors.navyBlue, CircleShape),
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
                            .background(AppTheme.colors.navyBlue, CircleShape),
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
    onTransitTypeClick: (typeId: String) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Walk | Bike | Maps | e-Scooter | Bus (5 slots)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransitModeGridButton(
                icon = R.drawable.ic_walk_mode,
                label = "Walk",
                onClick = { /* Walk action */ },
                modifier = Modifier.weight(1f),
            )
            TransitModeGridButton(
                icon = R.drawable.ic_bike_mode,
                label = "Bike",
                isActive = activeTransitTypes.contains(TravelMode.BICYCLE_RENT.name),
                onClick = { onTransitTypeClick(TravelMode.BICYCLE_RENT.name) },
                modifier = Modifier.weight(1f),
            )

            // Bus: single click → navigate to NearbyBusStopsSheet
            TransitModeGridButton(
                icon = R.drawable.ic_bus_mode,
                label = "Bus",
                isActive = activeTransitTypes.contains(TransitMode.BUS.name),
                onClick = { onTransitTypeClick(TransitMode.BUS.name) },
                modifier = Modifier.weight(1f),
            )
            TransitModeGridButton(
                icon = R.drawable.ic_metro_mode,
                label = "Metro",
                isActive = activeTransitTypes.contains(TransitMode.SUBWAY.name),
                onClick = { onTransitTypeClick(TransitMode.SUBWAY.name) },
                modifier = Modifier.weight(1f),
            )
            TransitModeGridButton(
                icon = R.drawable.ic_scooter_mode,
                label = "e-Scooter",
                onClick = { /* e-Scooter action */ },
                modifier = Modifier.weight(1f),
            )
        }

        // Row 2: Metro | Rail | More | Spacer | Spacer (5 slots)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransitModeGridButton(
                icon = R.drawable.ic_map,
                label = "Maps",
                onClick = { /* Open maps action */ },
                modifier = Modifier.weight(1f),
            )

            TransitModeGridButton(
                icon = Icons.Default.Train,
                label = "Rail",
                isActive = activeTransitTypes.contains(TransitMode.RAIL.name),
                onClick = { onTransitTypeClick(TransitMode.RAIL.name) },
                modifier = Modifier.weight(1f),
            )
            TransitModeGridButton(
                icon = Icons.Default.KeyboardArrowDown,
                label = "More",
                onClick = { /* More transit types */ },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}@Composable
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
        TransitModeGridButton(
            icon = Icons.Default.ConfirmationNumber,
            label = "Vé",
            onClick = onTicketClick,
            modifier = Modifier.weight(1f),
        )
        TransitModeGridButton(
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
private fun TransitModeGridButton(
    icon: Any,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    isActive: Boolean = true,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) AppTheme.colors.blackOverlay20 else AppTheme.colors.blackOverlay10,
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
                when (icon) {
                    is ImageVector -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    is Int -> {
                        Image(
                            painter = painterResource(id = icon),
                            contentDescription = label,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

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
