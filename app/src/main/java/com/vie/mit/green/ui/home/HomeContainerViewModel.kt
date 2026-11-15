package com.vie.mit.green.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.data.model.domain.map.TransitStopsGeoJsonLoad
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.repository.map.MapRepository
import com.vie.mit.green.ui.home.homesheet.HomeSheetUiState
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyStopItem
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.StopFeatureClick
import com.vie.mit.mapbox.model.TransitMapViewportInfo
import com.vie.mit.mapbox.model.TransitSourceData
import com.vie.mit.mapbox.model.TransitViewportBounds
import com.vie.mit.mapbox.model.bufferedRequestBounds
import com.vie.mit.mapbox.model.distanceTo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class HomeContainerViewModel @Inject constructor(
    private val mapRepository: MapRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeSheetUiState(transitConfigs = mapRepository.defaultConfigs)
    )
    val uiState: StateFlow<HomeSheetUiState> = _uiState.asStateFlow()

    // Smart caching state for camera viewport
    private var cachedBounds: List<TransitViewportBounds>? = null
    private var lastLoadedZoom: Double = 0.0
    private var debounceLoadJob: Job? = null
    private var fetchTransitDataJob: Job? = null
    private var computeNearbyStopsJob: Job? = null
    private var lastKnownLocation: LocationData? = null
    private var currentMapCenter: LocationData? = null
    private var hasLoadedInitialLocation: Boolean = false
    private var isNearbySheetVisible: Boolean = false

    companion object {
        private const val MIN_ZOOM_THRESHOLD = 11.0
        private const val BOUNDS_BUFFER_FACTOR = 0.35 // 35% buffer to reduce reload frequency
        private const val DEBOUNCE_DELAY_MS = 400L
    }

    /**
     * Called when the user's GPS location is first acquired or updated.
     * Caches the latest location without triggering continuous recomposition.
     * Only updates UI State on the very first fix to initialize data.
     */
    fun onLocationUpdated(location: LocationData) {
        lastKnownLocation = location

        if (!hasLoadedInitialLocation) {
            hasLoadedInitialLocation = true
            _uiState.update { currentState ->
                currentState.copy(
                    currentLocation = location,
                )
            }

            // Data loads from Mapbox's viewport callback after the camera focuses this location.
        }
    }

    /**
     * Returns the latest cached GPS location.
     */
    fun getLastKnownLocation(): LocationData? = lastKnownLocation

    /**
     * Updates user location on-demand (e.g. when clicking My Location button).
     */
    fun onUserLocationButtonClicked(location: LocationData) {
        _uiState.update { currentState ->
            currentState.copy(
                currentLocation = location,
            )
        }
    }

    /**
     * Evaluates camera movement and determines whether new data is needed.
     * Prevents unnecessary reloads when zooming in or panning within cached bounds.
     * Updates [currentMapCenter] directly from MapView camera state as Single Source of Truth.
     */
    fun onViewportChanged(viewport: TransitMapViewportInfo) {
        // Cập nhật tâm bản đồ trực tiếp từ MapView (SSOT)
        currentMapCenter = LocationData(viewport.centerLatitude, viewport.centerLongitude)
        debounceLoadJob?.cancel()
        debounceLoadJob = null

        if (viewport.zoom < MIN_ZOOM_THRESHOLD) {
            cancelTransitDataLoad()
            Timber.d("Skip load: current zoom (${viewport.zoom}) is below threshold ($MIN_ZOOM_THRESHOLD)")
            return
        }

        val cached = cachedBounds
        if (cached != null) {
            // Rule 1: If zooming in within cached bounds, do NOT reload!
            val isZoomingIn = viewport.zoom >= lastLoadedZoom
            val isWithinCachedBounds = viewport.bufferedRequestBounds(bufferFactor = 0.0).all { visibleBounds ->
                cached.any { cachedBound -> visibleBounds.isInside(cachedBound) }
            }

            if (isWithinCachedBounds && isZoomingIn) {
                cancelTransitDataLoad()
                Timber.d("Skip load: viewport is within cached bounds and user is zooming in")
                return
            }
        }

        // Debounce camera movements before fetching
        debounceLoadJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY_MS.milliseconds)
            loadTransitStopsData(viewport)
        }
    }

    /**
     * Called when user releases a gesture (long press / scroll / drag release).
     * Directly triggers transit data loading for the current camera viewport.
     */
    fun onCameraGestureRelease(viewport: TransitMapViewportInfo) {
        val centerLocation = LocationData(viewport.centerLatitude, viewport.centerLongitude)
        currentMapCenter = centerLocation

        // Tự động tính toán lại trạm theo tâm bản đồ mới khi kéo và nhả tay (nếu NearbyTransitSheet đang hiển thị)
        if (isNearbySheetVisible) {
            computeNearbyStopsForSheet()
        }

        if (viewport.zoom < MIN_ZOOM_THRESHOLD) {
            Timber.d("Skip load on gesture release: current zoom (${viewport.zoom}) is below threshold ($MIN_ZOOM_THRESHOLD)")
            return
        }
        debounceLoadJob?.cancel()
        loadTransitStopsData(viewport)
    }

    private fun loadTransitStopsData(viewport: TransitMapViewportInfo) {
        val bufferedBounds:List<TransitViewportBounds> = viewport.bufferedRequestBounds(BOUNDS_BUFFER_FACTOR)
        val requestBounds = bufferedBounds.map {
            TransitStopBoundsRequest(
                minLat = it.minLatitude,
                minLon = it.minLongitude,
                maxLat = it.maxLatitude,
                maxLon = it.maxLongitude,
            )
        }

        cancelTransitDataLoad()
        fetchTransitDataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTransit = true, errorMessage = null) }

            mapRepository.getTransitStopsGeoJson(requestBounds).onSuccess { load: TransitStopsGeoJsonLoad ->
                val geoJsonMap = load.geoJsonByType
                val activeTypes = _uiState.value.activeTransitTypes
                val sources = _uiState.value.transitConfigs.map { config ->
                    TransitSourceData(
                        typeId = config.typeId,
                        geoJson = geoJsonMap.getValue(config.typeId),
                        isVisible = activeTypes.contains(config.typeId),
                    )
                }
                if (load.failedTypeIds.isEmpty()) {
                    cachedBounds = bufferedBounds
                    lastLoadedZoom = viewport.zoom
                } else {
                    cachedBounds = null
                    Timber.w("Transit stop load was partial; will retry failed types: ${load.failedTypeIds}")
                }
                _uiState.update { currentState ->
                    currentState.copy(
                        transitSources = sources,
                        isLoadingTransit = false,
                    )
                }
//                if (isNearbySheetVisible) {
//                    computeNearbyStopsForSheet(_uiState.value.currentMapCenter)
//                }
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

    private fun cancelTransitDataLoad() {
        fetchTransitDataJob?.cancel()
        fetchTransitDataJob = null
        _uiState.update { it.clearTransitLoading() }
    }

    private fun TransitViewportBounds.isInside(container: TransitViewportBounds): Boolean =
        minLatitude >= container.minLatitude &&
                maxLatitude <= container.maxLatitude &&
                minLongitude >= container.minLongitude &&
                maxLongitude <= container.maxLongitude

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
     * Controls lifecycle visibility of NearbyTransitSheet.
     * Starts computing nearby stops when sheet appears, cancels ongoing computation when sheet disappears.
     */
    fun setNearbySheetVisible(isVisible: Boolean) {
        isNearbySheetVisible = isVisible
        if (isVisible) {
            computeNearbyStopsForSheet()
        } else {
            computeNearbyStopsJob?.cancel()
        }
    }

    /**
     * Computes nearby stops around [targetLocation] or the current map center/user location.
     * Runs asynchronously on Dispatchers.Default to avoid blocking the Main UI thread.
     * Called ONLY when NearbyTransitSheet is displayed.
     */
    fun computeNearbyStopsForSheet(targetLocation: LocationData? = null) {
        computeNearbyStopsJob?.cancel()
        computeNearbyStopsJob = viewModelScope.launch(Dispatchers.Default) {
            val currentState = _uiState.value
            val center = targetLocation
                ?: currentMapCenter
                ?: currentState.currentLocation
                ?: return@launch

            val stops = computeNearbyStops(
                sources = currentState.transitSources,
                centerLocation = center,
                radiusKm = currentState.selectedRadiusKm
            )

            _uiState.update { it.copy(nearbyStops = stops) }
        }
    }

    /**
     * Updates selected radius in KM for nearby transit stops filter.
     * Recomputes nearby stops for the sheet.
     */
    fun setSelectedRadiusKm(radiusKm: Double) {
        _uiState.update { it.copy(selectedRadiusKm = radiusKm) }
        computeNearbyStopsForSheet()
    }

    /**
     * Called when the map is clicked.
     * Sets map focus to true and updates clicked map location as center.
     */
    fun onMapClick(lat: Double? = null, lon: Double? = null) {
        _uiState.update { currentState ->
            currentState.copy(
                isMapFocused = true,
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

internal fun HomeSheetUiState.clearTransitLoading(): HomeSheetUiState =
    if (isLoadingTransit) copy(isLoadingTransit = false) else this
