package com.vie.mit.green.ui.busroutesearch

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vie.mit.common.theme.AppTheme

@Composable
fun BusRouteSearchScreen(
    viewModel: BusRouteSearchViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val busColor = AppTheme.colors.busColor

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF9FAFB),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::navigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color(0xFF1F2937),
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Tìm tuyến xe buýt",
                    style = AppTheme.typography.headlineMedium.copy(
                        color = Color(0xFF1F2937),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            // Search Input Field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Nhập mã tuyến hoặc tên tuyến...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9CA3AF),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = busColor,
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Xóa",
                                tint = Color(0xFF9CA3AF),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = busColor,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Results or Empty State
            if (uiState.routes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.searchQuery.isEmpty()) {
                                "Nhập mã số tuyến để tìm kiếm"
                            } else {
                                "Không tìm thấy tuyến xe phù hợp"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = uiState.routes,
                        key = { it.routeId },
                    ) { route ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(busColor, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = route.routeShortName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        ),
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = route.routeLongName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827),
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
