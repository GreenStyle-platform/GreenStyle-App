package com.vie.mit.green.ui.home.nearbysubwaystations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.repository.transit.TransitRouteRepository
import com.vie.mit.green.navigation.main_graph.TransitRouteDetailRoute
import com.vie.mit.green.navigation.main_graph.TransitStopDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for NearbySubwayStationsSheet.
 *
 * Responsibilities:
 * - Manages active tab state ([SubwaySheetTab.STATIONS] vs [SubwaySheetTab.ROUTES]).
 * - Calls [TransitRouteRepository] to fetch all subway/metro routes from the backend.
 * - Handles navigation to metro route details.
 */
@HiltViewModel
class NearbySubwayStationViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val transitRouteRepository: TransitRouteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NearbySubwayStationUiState())
    val uiState: StateFlow<NearbySubwayStationUiState> = _uiState.asStateFlow()

    private var loadRoutesJob: Job? = null

    /**
     * Switch between Stations tab and Routes tab.
     * When switching to Routes tab, automatically fetch metro routes if not already loaded.
     */
    fun onTabChanged(tab: SubwaySheetTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == SubwaySheetTab.ROUTES && _uiState.value.subwayRoutes.isEmpty() && !_uiState.value.isLoadingRoutes) {
            loadSubwayRoutes()
        }
    }

    /**
     * Fetches subway/metro routes from the backend.
     */
    fun loadSubwayRoutes(query: String = "") {
        loadRoutesJob?.cancel()
        _uiState.update {
            it.copy(
                isLoadingRoutes = true,
                routesErrorMessage = null,
            )
        }

        loadRoutesJob = viewModelScope.launch {
            transitRouteRepository.getSubwayRoutes(query = query).onSuccess { routes ->
                _uiState.update {
                    it.copy(
                        subwayRoutes = routes,
                        isLoadingRoutes = false,
                        routesErrorMessage = null,
                    )
                }
                Timber.d("NearbySubwayStationViewModel: loaded ${routes.size} subway routes")
            }.onFailure { error ->
                Timber.e(error, "NearbySubwayStationViewModel: failed to load subway routes")
                _uiState.update {
                    it.copy(
                        isLoadingRoutes = false,
                        routesErrorMessage = error.message
                            ?: "Không thể tải danh sách tuyến metro",
                    )
                }
            }
        }
    }

    /**
     * Navigates to the Transit Route Detail screen for the clicked subway route.
     */
    fun navigateToRouteDetail(routeId: String) {
        if (routeId.isBlank()) return
        appNavigator.navigateTo(
            TransitRouteDetailRoute(
                routeId = routeId,
                transitMode = TransitMode.SUBWAY,
            )
        ) {
            launchSingleTop = true
        }
    }

    fun navigateToTransitStopDetail(stopId: String) {
        if (stopId.isBlank()) return
        appNavigator.navigateTo(
            TransitStopDetailRoute(
                stopId = stopId,
                transitMode = TransitMode.SUBWAY,
            )
        ) {
            launchSingleTop = true
        }
    }
}
