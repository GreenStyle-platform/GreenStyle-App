package com.vie.mit.green.ui.transitstopdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.data.repository.map.MapRepository
import com.vie.mit.green.navigation.main_graph.TransitRouteDetailRoute
import com.vie.mit.green.navigation.main_graph.TransitStopDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TransitStopDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mapRepository: MapRepository,
    private val appNavigator: AppNavigator,
) : ViewModel() {
    private val route: TransitStopDetailRoute = savedStateHandle.toRoute<TransitStopDetailRoute>()
    private val _uiState = MutableStateFlow(
        TransitStopDetailUiState(
            stopId = route.stopId,
            type = route.transitMode,
            typeId = route.transitMode.name,
            isLoading = true,
        )
    )
    val uiState: StateFlow<TransitStopDetailUiState> = _uiState.asStateFlow()

    init {
        loadStopDetail()
    }

    fun loadStopDetail() {
        val stopId = route.stopId
        if (stopId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Mã trạm không hợp lệ") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            mapRepository.getTransitStopDetail(stopId = stopId, type = route.transitMode)
                .onSuccess { detail ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            stopName = if (detail.name.isNotBlank()) detail.name else current.stopName,
                            latitude = if (detail.latitude != 0.0) detail.latitude else current.latitude,
                            longitude = if (detail.longitude != 0.0) detail.longitude else current.longitude,
                            address = detail.address,
                            routeArrivals = detail.routeArrivals,
                            error = null,
                        )
                    }
                }.onFailure { error ->
                    Timber.e(error, "Failed to load transit stop detail for stopId: $stopId")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Không thể tải thông tin trạm dừng",
                        )
                    }
                }
        }
    }

    fun navigateBack() {
        appNavigator.navigateUp()
    }

    fun navigateToDetailRoute(routeId: String) {
        if (routeId.isBlank()) return
        appNavigator.navigateTo(
            TransitRouteDetailRoute(
                routeId = routeId,
                transitMode = uiState.value.type,
            )
        ) {
            launchSingleTop = true
        }
    }
}
