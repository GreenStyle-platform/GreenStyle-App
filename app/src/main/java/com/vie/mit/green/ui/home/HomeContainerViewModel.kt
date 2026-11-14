package com.vie.mit.green.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.data.repository.map.MapRepository
import com.vie.mit.green.ui.home.homesheet.HomeSheetUiState
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyStopItem
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.StopFeatureClick
import com.vie.mit.mapbox.model.TransitMapViewportInfo
import com.vie.mit.mapbox.model.TransitSourceData
import com.vie.mit.mapbox.model.distanceTo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeContainerViewModel @Inject constructor(
    private val mapRepository: MapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeSheetUiState(transitConfigs = mapRepository.defaultConfigs)
    )
    val uiState: StateFlow<HomeSheetUiState> = _uiState.asStateFlow()

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
        _uiState.update { currentState ->
            val mapCenter = currentState.currentMapCenter ?: location
            val updatedState = currentState.copy(
                currentLocation = location,
                currentMapCenter = mapCenter,
            )
            updatedState.copy(
                nearbyStops = computeNearbyStops(updatedState.transitSources, mapCenter, updatedState.selectedRadiusKm)
            )
        }

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
        val centerLocation = LocationData(viewport.centerLatitude, viewport.centerLongitude)
        _uiState.update { currentState ->
            currentState.copy(
                currentMapCenter = centerLocation,
                nearbyStops = computeNearbyStops(currentState.transitSources, centerLocation, currentState.selectedRadiusKm)
            )
        }

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

    /**
     * Called when user releases a gesture (long press / scroll / drag release).
     * Directly triggers transit data loading for the current camera viewport.
     */
    fun onCameraGestureRelease(viewport: TransitMapViewportInfo) {
        if (viewport.zoom < MIN_ZOOM_THRESHOLD) {
            Timber.d("Skip load on gesture release: current zoom (${viewport.zoom}) is below threshold ($MIN_ZOOM_THRESHOLD)")
            return
        }
        debounceLoadJob?.cancel()
        triggerDataLoad(viewport)
    }

    fun onViewportGestureReleased(viewport: TransitMapViewportInfo) = onCameraGestureRelease(viewport)

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
                _uiState.update { currentState ->
                    currentState.copy(
                        transitSources = sources,
                        isLoadingTransit = false,
                        nearbyStops = computeNearbyStops(
                            sources = sources,
                            centerLocation = currentState.currentMapCenter ?: currentState.currentLocation,
                            radiusKm = currentState.selectedRadiusKm
                        )
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

    /**
     * Updates selected radius in KM for nearby transit stops filter.
     */
    fun setSelectedRadiusKm(radiusKm: Double) {
        _uiState.update { currentState ->
            val center = currentState.currentMapCenter ?: currentState.currentLocation
            currentState.copy(
                selectedRadiusKm = radiusKm,
                nearbyStops = computeNearbyStops(currentState.transitSources, center, radiusKm)
            )
        }
    }

    /**
     * Called when the map is clicked.
     * Sets map focus to true and updates clicked map location as center.
     */
    fun onMapClick(lat: Double? = null, lon: Double? = null) {
        _uiState.update { currentState ->
            val clickedCenter = if (lat != null && lon != null) {
                LocationData(lat, lon)
            } else currentState.currentMapCenter ?: currentState.currentLocation

            currentState.copy(
                isMapFocused = true,
                currentMapCenter = clickedCenter,
                nearbyStops = computeNearbyStops(currentState.transitSources, clickedCenter, currentState.selectedRadiusKm)
            )
        }
    }

    fun showHomeBottomSheet() {
        _uiState.update { currentState ->
            currentState.copy(
                isMapFocused = false,
            )
        }
    }

    fun showNearbyStopsBottomSheet() {
        _uiState.update { currentState ->
            currentState.copy(
                isMapFocused = false,
            )
        }
    }

    fun setMapFocused(isFocused: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isMapFocused = isFocused)
        }
    }

    private fun computeNearbyStops(
        sources: List<TransitSourceData>,
        centerLocation: LocationData?,
        radiusKm: Double,
    ): List<NearbyStopItem> {
        if (centerLocation == null) return emptyList()
        val maxDistanceMeters = radiusKm * 1000.0
        val allStops = mutableListOf<NearbyStopItem>()

        for (source in sources) {
            if (!source.isVisible) continue
            val stops = parseStopsFromGeoJson(source.geoJson, source.typeId, centerLocation)
            allStops.addAll(stops)
        }

        return allStops
            .filter { it.distanceMeters <= maxDistanceMeters }
            .sortedBy { it.distanceMeters }
    }

    private fun parseStopsFromGeoJson(
        geoJsonStr: String,
        defaultTypeId: String,
        centerLocation: LocationData,
    ): List<NearbyStopItem> {
        val result = mutableListOf<NearbyStopItem>()
        try {
            val json = JSONObject(geoJsonStr)
            val features = json.optJSONArray("features") ?: return emptyList()
            for (i in 0 until features.length()) {
                val feature = features.optJSONObject(i) ?: continue
                val geometry = feature.optJSONObject("geometry") ?: continue
                val coordinates = geometry.optJSONArray("coordinates") ?: continue
                if (coordinates.length() < 2) continue
                val lon = coordinates.optDouble(0)
                val lat = coordinates.optDouble(1)
                val stopLocation = LocationData(lat, lon)
                val dist = stopLocation.distanceTo(centerLocation)

                val propertiesObj = feature.optJSONObject("properties")
                val propertiesMap = mutableMapOf<String, String>()
                var stopId = feature.optString("id", "stop_$i")
                var name = ""
                var typeId = defaultTypeId
                var routesOrInfo = ""

                if (propertiesObj != null) {
                    val keys = propertiesObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        propertiesMap[key] = propertiesObj.optString(key, "")
                    }
                    if (propertiesMap.containsKey("stop_id")) stopId = propertiesMap["stop_id"] ?: stopId
                    name = propertiesMap["name"] ?: ""
                    typeId = propertiesMap["type"] ?: defaultTypeId
                    routesOrInfo = propertiesMap["routes"]
                        ?: propertiesMap["line"]
                        ?: propertiesMap["available_bikes"]?.let { "Xe khả dụng: $it" }
                        ?: ""
                }

                if (name.isBlank()) {
                    name = "Trạm $typeId ($stopId)"
                }

                result.add(
                    NearbyStopItem(
                        stopId = stopId,
                        name = name,
                        typeId = typeId,
                        location = stopLocation,
                        distanceMeters = dist,
                        routesOrInfo = routesOrInfo,
                        properties = propertiesMap,
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing GeoJSON for nearby stops")
        }
        return result
    }
}
