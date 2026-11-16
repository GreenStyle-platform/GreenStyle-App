package com.vie.mit.green.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.data.model.domain.map.TransitStopsGeoJsonLoad
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.repository.map.MapRepository
import com.vie.mit.green.navigation.main_graph.SettingRoute
import com.vie.mit.green.ui.home.homesheet.HomeSheetUiState
import com.vie.mit.green.ui.home.nearbytransitsheet.NearbyStopItem
import com.vie.mit.mapbox.model.LocationData
import com.vie.mit.mapbox.model.StopFeatureClick
import com.vie.mit.mapbox.model.TransitMapViewportInfo
import com.vie.mit.mapbox.model.TransitSourceData
import com.vie.mit.mapbox.model.TransitViewportBounds
import com.vie.mit.mapbox.model.bufferedRequestBounds
import com.vie.mit.mapbox.model.distanceTo
import com.vie.mit.mapbox.util.isInside
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
    private val mapRepository: MapRepository, private val appNavigator: AppNavigator
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeSheetUiState(transitConfigs = mapRepository.defaultConfigs)
    )
    val uiState: StateFlow<HomeSheetUiState> = _uiState.asStateFlow()

    // Smart caching state per transit type for camera viewport
    private val cachedBoundsByType = mutableMapOf<String, List<TransitViewportBounds>>()
    private val lastLoadedZoomByType = mutableMapOf<String, Double>()
    private val cachedGeoJsonByType = mutableMapOf<String, String>()

    private var debounceLoadJob: Job? = null
    private var fetchTransitDataJob: Job? = null
    private var computeNearbyStopsJob: Job? = null
    private var lastKnownLocation: LocationData? = null
    private var currentMapCenter: LocationData? = null
    private var hasLoadedInitialLocation: Boolean = false
    private var isNearbySheetVisible: Boolean = false

    companion object {
        private const val MIN_ZOOM_THRESHOLD = 8.0
        private const val BOUNDS_BUFFER_FACTOR = 0.25 // 35% buffer to reduce reload frequency
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

    private fun shouldFetchForType(typeId: String, viewport: TransitMapViewportInfo): Boolean {
        val cached = cachedBoundsByType[typeId] ?: return true
        val lastZoom = lastLoadedZoomByType[typeId] ?: 0.0
        val isZoomingIn = viewport.zoom >= lastZoom
        val isWithinCachedBounds =
            viewport.bufferedRequestBounds(bufferFactor = 0.0).all { visibleBounds ->
                cached.any { cachedBound -> visibleBounds.isInside(cachedBound) }
        }
        return !(isWithinCachedBounds && isZoomingIn)
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
        val typesNeedingFetch = _uiState.value.transitConfigs.map { it.typeId }
            .filter { shouldFetchForType(it, viewport) }

        if (typesNeedingFetch.isEmpty()) {
            cancelTransitDataLoad()
            Timber.d("PhucTH: LoadStopDebug: skipped — all transit types cached for this viewport")
            return
        }

        // Debounce camera movements before fetching
        debounceLoadJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY_MS.milliseconds)
            loadTransitStopsData(viewport, typesNeedingFetch.toSet())
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
        val typesNeedingFetch = _uiState.value.transitConfigs.map { it.typeId }
            .filter { shouldFetchForType(it, viewport) }

        if (typesNeedingFetch.isEmpty()) {
            Timber.d("Skip load on gesture release: all transit types cached for this viewport")
            return
        }

        debounceLoadJob?.cancel()
        loadTransitStopsData(viewport, typesNeedingFetch.toSet())
    }

    private fun loadTransitStopsData(
        viewport: TransitMapViewportInfo,
        typeIdsToFetch: Set<String> = _uiState.value.transitConfigs.map { it.typeId }.toSet(),
    ) {
        if (typeIdsToFetch.isEmpty()) return
        val bufferedBounds: List<TransitViewportBounds> =
            viewport.bufferedRequestBounds(BOUNDS_BUFFER_FACTOR)
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

            mapRepository.getTransitStopsGeoJson(requestBounds, typeIdsToFetch)
                .onSuccess { load: TransitStopsGeoJsonLoad ->
                    val fetchedGeoJsonMap = load.geoJsonByType

                    typeIdsToFetch.forEach { typeId ->
                        if (!load.failedTypeIds.contains(typeId)) {
                            val geoJson = fetchedGeoJsonMap[typeId].orEmpty()
                            cachedGeoJsonByType[typeId] = geoJson
                            cachedBoundsByType[typeId] = bufferedBounds
                            lastLoadedZoomByType[typeId] = viewport.zoom
                        }
                    }

                    if (load.failedTypeIds.isNotEmpty()) {
                        Timber.w("Transit stop load was partial; failed types: ${load.failedTypeIds}")
                    }

                val activeTypes = _uiState.value.activeTransitTypes
                val sources = _uiState.value.transitConfigs.map { config ->
                    TransitSourceData(
                        typeId = config.typeId,
                        geoJson = cachedGeoJsonByType[config.typeId].orEmpty(),
                        isVisible = activeTypes.contains(config.typeId),
                    )
                }
                _uiState.update { currentState ->
                    currentState.copy(
                        transitSources = sources,
                        isLoadingTransit = false,
                    )
                }

                    if (isNearbySheetVisible) {
                        computeNearbyStopsForSheet()
                    }
                    Timber.d("Successfully loaded GTFS data for ${typeIdsToFetch.size - load.failedTypeIds.size} fetched transit types")
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

    fun navigateToSettingScreen() {
        appNavigator.navigateTo(SettingRoute)
    }
}

internal fun HomeSheetUiState.clearTransitLoading(): HomeSheetUiState =
    if (isLoadingTransit) copy(isLoadingTransit = false) else this
