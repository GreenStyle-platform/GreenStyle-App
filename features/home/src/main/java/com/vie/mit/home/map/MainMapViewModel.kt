package com.vie.mit.home.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.data.network.model.map.Location
import com.vie.mit.data.repository.map.MapRepository
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.distanceTo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainMapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
) : ViewModel() {
    private val _uiState: MutableStateFlow<MainMapUiState> = MutableStateFlow(MainMapUiState())
    val uiState: StateFlow<MainMapUiState> = _uiState.asStateFlow()

    private var lastFetchedLocation: LocationData? = null

    companion object {
        private const val SIGNIFICANT_DISTANCE_THRESHOLD_METERS = 50f
    }

    fun onLocationUpdated(locationData: LocationData) {
        Timber.d("MainMapViewModel location updated: $locationData")
        _uiState.update { currentState ->
            currentState.copy(currentLocation = locationData)
        }
        updateSuggestionLocation(locationData)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSearch(query: String) {
        Timber.d("Searching for location: $query")
    }

    fun updateSuggestionLocation(locationData: LocationData) {
        val lastLocation = lastFetchedLocation
        if ((lastLocation != null) && (lastLocation.distanceTo(locationData) < SIGNIFICANT_DISTANCE_THRESHOLD_METERS)) {
            Timber.d("Skip fetching suggestions: movement (${lastLocation.distanceTo(locationData)}m) is less than threshold ($SIGNIFICANT_DISTANCE_THRESHOLD_METERS m)")
            return
        }

        lastFetchedLocation = locationData
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSuggestions = true, suggestionsError = null) }
            val location = Location(
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                altitude = locationData.altitude,
            )
            mapRepository.getPublicTransportSuggestions(location)
                .onSuccess { suggestions ->
                    Timber.d("Public transport suggestions received: ${suggestions.size}")
                    _uiState.update { currentState ->
                        currentState.copy(
                            publicTransportSuggestions = suggestions,
                            isLoadingSuggestions = false,
                        )
                    }
                }
                .onFailure { exception ->
                    Timber.e(exception, "Failed to fetch public transport suggestions")
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoadingSuggestions = false,
                            suggestionsError = exception.message ?: "Failed to load suggestions",
                        )
                    }
                }
        }
    }
}
