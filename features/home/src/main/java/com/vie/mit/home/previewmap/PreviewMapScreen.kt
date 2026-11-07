package com.vie.mit.home.previewmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vie.mit.common.theme.AppTheme
import com.vie.mit.common.ui.textfield.SearchTextFieldGre
import com.vie.mit.data.network.model.map.PublicTransportSuggestion
import com.vie.mit.mapbox.component.CurrentLocationMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewMapScreen(viewModel: PreviewMapViewModel) {

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 80.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetTonalElevation = 8.dp,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = null,
        sheetSwipeEnabled = true,
        sheetContent = {
            BottomSheetContent(viewModel)
        },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            CurrentLocationMap(
                modifier = Modifier.fillMaxSize(),
                onLocationUpdate = viewModel::onLocationUpdated,
            )

        }
    }
}

@Composable
private fun BottomSheetContent(viewModel: PreviewMapViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {

        HorizontalDivider(modifier = Modifier.height(4.dp).width(80.dp).background(AppTheme.colors.disabledColor))
        SearchTextFieldGre(
            value = uiState.searchQuery,
            onValueChange = { query ->
                viewModel.onSearchQueryChanged(query)
            },
            placeholder = "Tìm kiếm điểm đến...",
            onSearch = { query ->
                viewModel.onSearch(query)
            }
        )
        // Current Location Header
        Text(
            text = "Vị trí hiện tại",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        val currentLocation = uiState.currentLocation
        if (currentLocation != null) {
            Text(
                text = "Lat: ${currentLocation.latitude}, Lng: ${currentLocation.longitude}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "Đang xác định vị trí...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Gợi ý phương tiện công cộng",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val suggestionsError = uiState.suggestionsError

        when {
            uiState.isLoadingSuggestions -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            suggestionsError != null -> {
                Text(
                    text = suggestionsError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            uiState.publicTransportSuggestions.isEmpty() -> {
                Text(
                    text = "Không có gợi ý nào quanh vị trí này",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.publicTransportSuggestions) { item ->
                        PublicTransportItem(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicTransportItem(item: PublicTransportSuggestion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Loại: ${item.type.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${item.distance} m",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Còn ${item.availableIn} phút",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
