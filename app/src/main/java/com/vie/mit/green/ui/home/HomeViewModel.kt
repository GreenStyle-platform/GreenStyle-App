package com.vie.mit.green.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.data.repository.map.MapRepository
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.StopFeatureClick
import com.vie.mit.mapbox.model.TransitMapViewportInfo
import com.vie.mit.mapbox.model.TransitSourceData
import com.vie.mit.mapbox.model.TransitTypeConfig
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
class HomeViewModel @Inject constructor(
    private val mapRepository: MapRepository,
) : ViewModel() {

    private val defaultConfigs = listOf(
        TransitTypeConfig(
            typeId = "BUS",
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bus,
            clusterColor = 0xFF1E88E5.toInt(), // Blue
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
        TransitTypeConfig(
            typeId = "METRO",
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_metro,
            clusterColor = 0xFFE53935.toInt(), // Red
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
        TransitTypeConfig(
            typeId = "TRAIN",
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_train,
            clusterColor = 0xFF8E24AA.toInt(), // Purple
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
        TransitTypeConfig(
            typeId = "BIKE",
            iconResId = com.vie.mit.mapbox.R.drawable.ic_transit_bike,
            clusterColor = 0xFF43A047.toInt(), // Green
            clusterTextColor = 0xFFFFFFFF.toInt(),
            iconSize = 1.0,
        ),
    )

    private val _uiState = MutableStateFlow(
        HomeUiState(transitConfigs = defaultConfigs)
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Smart caching state for camera viewport
    private var cachedBounds: TransitMapViewportInfo? = null
    private var lastLoadedZoom: Double = 0.0
    private var debounceLoadJob: Job? = null

    companion object {
        private const val MIN_ZOOM_THRESHOLD = 11.0
        private const val BOUNDS_BUFFER_FACTOR = 0.35 // 35% buffer to reduce reload frequency
        private const val DEBOUNCE_DELAY_MS = 400L
    }

    /**
     * Called when the user's GPS location is first acquired or updated.
     */
    fun onLocationUpdated(location: LocationData) {
        _uiState.update { it.copy(currentLocation = location) }

        // Initial load based on user location if we haven't loaded any data yet
        if (cachedBounds == null) {
            val delta = 0.025 // ~2.5km radius bounding box
            val initialBounds = TransitMapViewportInfo(
                minLatitude = location.latitude - delta,
                minLongitude = location.longitude - delta,
                maxLatitude = location.latitude + delta,
                maxLongitude = location.longitude + delta,
                zoom = 14.0,
                centerLatitude = location.latitude,
                centerLongitude = location.longitude,
            )
            triggerDataLoad(initialBounds)
        }
    }

    /**
     * Evaluates camera movement and determines whether new data is needed.
     * Prevents unnecessary reloads when zooming in or panning within cached bounds.
     */
    fun onViewportChanged(viewport: TransitMapViewportInfo) {
        if (viewport.zoom < MIN_ZOOM_THRESHOLD) {
            Timber.d("Skip load: current zoom (${viewport.zoom}) is below threshold ($MIN_ZOOM_THRESHOLD)")
            return
        }

        val cached = cachedBounds
        if (cached != null) {
            // Rule 1: If zooming in within cached bounds, do NOT reload!
            val isZoomingIn = viewport.zoom >= lastLoadedZoom
            val isWithinCachedBounds = viewport.minLatitude >= cached.minLatitude &&
                    viewport.maxLatitude <= cached.maxLatitude &&
                    viewport.minLongitude >= cached.minLongitude &&
                    viewport.maxLongitude <= cached.maxLongitude

            if (isWithinCachedBounds && isZoomingIn) {
                Timber.d("Skip load: viewport is within cached bounds and user is zooming in")
                return
            }

            // Rule 2: If user is panning within cached bounds, do NOT reload!
            if (isWithinCachedBounds && (lastLoadedZoom - viewport.zoom) < 1.0) {
                Timber.d("Skip load: camera moved within loaded buffer area")
                return
            }
        }

        // Debounce camera movements before fetching
        debounceLoadJob?.cancel()
        debounceLoadJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY_MS)
            triggerDataLoad(viewport)
        }
    }

    private fun triggerDataLoad(viewport: TransitMapViewportInfo) {
        // Expand bounding box with buffer factor to preload neighboring area
        val latSpan = viewport.maxLatitude - viewport.minLatitude
        val lonSpan = viewport.maxLongitude - viewport.minLongitude
        val bufferedBounds = TransitMapViewportInfo(
            minLatitude = viewport.minLatitude - latSpan * BOUNDS_BUFFER_FACTOR,
            minLongitude = viewport.minLongitude - lonSpan * BOUNDS_BUFFER_FACTOR,
            maxLatitude = viewport.maxLatitude + latSpan * BOUNDS_BUFFER_FACTOR,
            maxLongitude = viewport.maxLongitude + lonSpan * BOUNDS_BUFFER_FACTOR,
            zoom = viewport.zoom,
            centerLatitude = viewport.centerLatitude,
            centerLongitude = viewport.centerLongitude,
        )

        cachedBounds = bufferedBounds
        lastLoadedZoom = viewport.zoom

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTransit = true, errorMessage = null) }

            mapRepository.getTransitStopsGeoJson(
                minLat = bufferedBounds.minLatitude,
                minLon = bufferedBounds.minLongitude,
                maxLat = bufferedBounds.maxLatitude,
                maxLon = bufferedBounds.maxLongitude,
                zoom = bufferedBounds.zoom,
                types = _uiState.value.activeTransitTypes.toList(),
            ).onSuccess { geoJsonMap ->
                val activeTypes = _uiState.value.activeTransitTypes
                val sources = geoJsonMap.map { (typeId, geoJsonStr) ->
                    TransitSourceData(
                        typeId = typeId,
                        geoJson = geoJsonStr,
                        isVisible = activeTypes.contains(typeId),
                    )
                }
                _uiState.update {
                    it.copy(
                        transitSources = sources,
                        isLoadingTransit = false,
                    )
                }
                Timber.d("Successfully loaded GTFS data for ${sources.size} transit types")
            }.onFailure { error ->
                Timber.e(error, "Failed to load GTFS transit stops")
                _uiState.update {
                    it.copy(
                        isLoadingTransit = false,
                        errorMessage = error.message ?: "Failed to load transit stops",
                    )
                }
            }
        }
    }

    /**
     * Toggles visibility for a given transit vehicle type (BUS, METRO, etc.).
     */
    fun toggleTransitType(typeId: String) {
        _uiState.update { currentState ->
            val updatedActive = if (currentState.activeTransitTypes.contains(typeId)) {
                currentState.activeTransitTypes - typeId
            } else {
                currentState.activeTransitTypes + typeId
            }

            val updatedSources = currentState.transitSources.map { source ->
                source.copy(isVisible = updatedActive.contains(source.typeId))
            }

            currentState.copy(
                activeTransitTypes = updatedActive,
                transitSources = updatedSources,
            )
        }
    }

    fun onStopClicked(click: StopFeatureClick) {
        Timber.d("Stop clicked: ${click.stopId} (${click.name})")
        _uiState.update { it.copy(selectedStop = click) }
    }

    fun dismissStopDetail() {
        _uiState.update { it.copy(selectedStop = null) }
    }
}
