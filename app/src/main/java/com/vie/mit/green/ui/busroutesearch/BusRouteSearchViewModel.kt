package com.vie.mit.green.ui.busroutesearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.data.repository.map.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BusRouteSearchViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val mapRepository: MapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusRouteUiState())
    val uiState: StateFlow<BusRouteUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        searchRoutes("")
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300L)
            searchRoutes(query)
        }
    }

    fun searchRoutes(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            mapRepository.searchBusRoutes(query)
                .onSuccess { routes ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            routes = routes,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to search bus routes")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Không thể tìm kiếm tuyến xe buýt",
                        )
                    }
                }
        }
    }

    fun navigateBack() {
        appNavigator.navigateUp()
    }
}
