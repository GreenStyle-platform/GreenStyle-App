package com.vie.mit.green.ui.busroutesearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.data.model.domain.map.BusRoute
import com.vie.mit.data.repository.map.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class BusRouteSearchViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val mapRepository: MapRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val busRoutesState: Flow<PagingData<BusRoute>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            mapRepository.getBusRoutesStream(query)
        }
        .cachedIn(viewModelScope)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun navigateBack() {
        appNavigator.navigateUp()
    }
}
