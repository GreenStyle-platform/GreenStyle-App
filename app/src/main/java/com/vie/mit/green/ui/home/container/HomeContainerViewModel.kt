package com.vie.mit.green.ui.home.container

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.common.map.TravelMode
import com.vie.mit.data.model.domain.map.TransitStopsGeoJsonLoad
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.repository.map.MapRepository
import com.vie.mit.green.navigation.main_graph.BusRouteSearchRoute
import com.vie.mit.green.navigation.main_graph.RoutePlannerContainerRoute
import com.vie.mit.green.navigation.main_graph.SettingRoute
import com.vie.mit.green.navigation.main_graph.TransitStopDetailRoute
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class CameraFocusCommand(
    val location: LocationData,
    val zoom: Double = 14.5,
    val animate: Boolean = true,
)

private data class ParsedTransitStop(
    val stopId: String,
    val name: String,
    val typeId: String,
    val location: LocationData,
    val routesOrInfo: String,
    val properties: Map<String, String>,
)

@HiltViewModel
class HomeContainerViewModel @Inject constructor(
    private val mapRepository: MapRepository, private val appNavigator: AppNavigator
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeContainerUiState(transitConfigs = mapRepository.defaultConfigs)
    )
    val uiState: StateFlow<HomeContainerUiState> = _uiState.asStateFlow()

    private val _cameraFocusEvents = MutableSharedFlow<CameraFocusCommand>(extraBufferCapacity = 1)
    val cameraFocusEvents: SharedFlow<CameraFocusCommand> = _cameraFocusEvents.asSharedFlow()

    // Smart caching state per transit type for camera viewport
    private val cachedBoundsByType = mutableMapOf<String, List<TransitViewportBounds>>()
    private val cachedGeoJsonByType = mutableMapOf<String, String>()
    private val cachedParsedStopsByType = mutableMapOf<String, List<ParsedTransitStop>>()

    private var debounceLoadJob: Job? = null
    private var fetchTransitDataJob: Job? = null
    private var computeNearbyStopsJob: Job? = null
    private var updateRadiusJob: Job? = null
    private var lastKnownLocation: LocationData? = null
    private var currentMapCenter: LocationData? = null
    private var currentViewport: TransitMapViewportInfo? = null
    private var hasLoadedInitialLocation: Boolean = false
    private var isNearbySheetVisible: Boolean = false

    companion object {
        private const val MIN_ZOOM_THRESHOLD = 8.0
        private const val BOUNDS_BUFFER_FACTOR = 0.25 // 25% buffer to reduce reload frequency
        private const val DEBOUNCE_DELAY_MS = 400L
        private const val NEARBY_RECOMPUTE_DELAY_MS = 200L
    }

    /**
     * Called when the user's GPS location is first acquired or updated.
     * Caches the latest location without triggering continuous recomposition.
     * Only updates UI State and emits a one-shot camera focus event on the very first fix.
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
            _cameraFocusEvents.tryEmit(
                CameraFocusCommand(
                    location = location,
                    zoom = 14.5,
                    animate = true,
                )
            )
            // Data loads from Mapbox's viewport callback after the camera focuses this location.
        }
    }

    /**
     * Returns the latest cached GPS location.
     */
    fun getLastKnownLocation(): LocationData? = lastKnownLocation

    /**
     * Returns the last valid map viewport so [HomeContainerScreen] can restore
     * the exact camera center & zoom immediately when returning from another destination.
     */
    fun getSavedCameraViewport(): TransitMapViewportInfo? =
        currentViewport?.takeIf { it.zoom >= MIN_ZOOM_THRESHOLD }

    /**
     * Updates user location on-demand (e.g. when clicking My Location button),
     * emits a single camera focus event, and recomputes nearby stops if a nearby sheet is visible.
     */
    fun onUserLocationButtonClicked(location: LocationData) {
        currentMapCenter = location
        _uiState.update { currentState ->
            currentState.copy(
                currentLocation = location,
            )
        }
        _cameraFocusEvents.tryEmit(
            CameraFocusCommand(
                location = location,
                zoom = 15.0,
                animate = true,
            )
        )
        if (isNearbySheetVisible) {
            updateRadiusJob?.cancel()
            computeNearbyStopsForSheet(targetLocation = location)
        }
    }

    internal fun shouldFetchForType(typeId: String, viewport: TransitMapViewportInfo): Boolean {
        val cached = cachedBoundsByType[typeId] ?: return true
        val isWithinCachedBounds =
            viewport.bufferedRequestBounds(bufferFactor = 0.0).all { visibleBounds ->
                cached.any { cachedBound -> visibleBounds.isInside(cachedBound) }
            }
        return !isWithinCachedBounds
    }

    /**
     * Evaluates camera movement and determines whether new data is needed.
     * Prevents unnecessary reloads when panning/zooming within cached bounds.
     * Updates [currentMapCenter] directly from MapView camera state as Single Source of Truth.
     */
    fun onViewportChanged(viewport: TransitMapViewportInfo) {
        // Cập nhật tâm bản đồ trực tiếp từ MapView (SSOT)
        currentMapCenter = LocationData(viewport.centerLatitude, viewport.centerLongitude)
        currentViewport = viewport
        debounceLoadJob?.cancel()
        debounceLoadJob = null

        if (viewport.zoom < MIN_ZOOM_THRESHOLD) {
            cancelTransitDataLoad()
            Timber.d("Skip load: current zoom (${viewport.zoom}) is below threshold ($MIN_ZOOM_THRESHOLD)")
            return
        }

        // Khi đang ở Sheet chuyên biệt (selectedTransitTypeId != null), CHỈ fetch đúng loại phương tiện đó
        val focusedType = _uiState.value.selectedTransitTypeId
        val typesNeedingFetch = if (focusedType != null) {
            if (shouldFetchForType(focusedType, viewport)) listOf(focusedType) else emptyList()
        } else {
            _uiState.value.transitConfigs.map { it.typeId }
                .filter { shouldFetchForType(it, viewport) }
        }

        if (typesNeedingFetch.isEmpty()) {
            cancelTransitDataLoad()
            // Recompute nearby stops after programmatic camera moves (e.g. flyTo) within cached bounds
            if (isNearbySheetVisible) {
                scheduleNearbyRecompute()
            }
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
     * Called when user releases a gesture (scroll / drag release).
     * Immediately recomputes nearby stops for the new map center when a nearby sheet is visible.
     */
    fun onCameraGestureRelease(viewport: TransitMapViewportInfo) {
        currentMapCenter = LocationData(viewport.centerLatitude, viewport.centerLongitude)
        currentViewport = viewport

        // Tự động tính toán lại trạm theo tâm bản đồ mới khi kéo và nhả tay (nếu NearbySheet đang hiển thị)
        if (isNearbySheetVisible) {
            updateRadiusJob?.cancel()
            computeNearbyStopsForSheet()
        }
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
                            cachedParsedStopsByType[typeId] = parseStopsFromGeoJson(geoJson, typeId)
                            cachedBoundsByType[typeId] = bufferedBounds
                        }
                    }

                    if (load.failedTypeIds.isNotEmpty()) {
                        Timber.w("Transit stop load was partial; failed types: ${load.failedTypeIds}")
                    }

                    val sources = buildTransitSources(
                        configs = _uiState.value.transitConfigs,
                        activeTypes = _uiState.value.activeTransitTypes,
                        selectedType = _uiState.value.selectedTransitTypeId,
                    )
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

    private fun buildTransitSources(
        configs: List<com.vie.mit.mapbox.model.TransitTypeConfig>,
        activeTypes: Set<String>,
        selectedType: String?,
    ): List<TransitSourceData> = configs.map { config ->
        TransitSourceData(
            typeId = config.typeId,
            geoJson = cachedGeoJsonByType[config.typeId].orEmpty(),
            isVisible = if (selectedType != null) {
                config.typeId == selectedType
            } else {
                activeTypes.contains(config.typeId)
            },
        )
    }

    private fun cancelTransitDataLoad() {
        fetchTransitDataJob?.cancel()
        fetchTransitDataJob = null
        _uiState.update { it.clearTransitLoading() }
    }

    private fun scheduleNearbyRecompute() {
        updateRadiusJob?.cancel()
        updateRadiusJob = viewModelScope.launch {
            delay(NEARBY_RECOMPUTE_DELAY_MS.milliseconds)
            computeNearbyStopsForSheet()
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

            val updatedSources = buildTransitSources(
                configs = currentState.transitConfigs,
                activeTypes = updatedActive,
                selectedType = currentState.selectedTransitTypeId,
            )

            currentState.copy(
                activeTransitTypes = updatedActive,
                transitSources = updatedSources,
            )
        }
        if (isNearbySheetVisible && _uiState.value.selectedTransitTypeId == null) {
            computeNearbyStopsForSheet()
        }
    }

    /**
     * Single source of truth for synchronizing map layers, nearby stop computation,
     * and data fetching whenever the active sheet route changes in [HomeContainerScreen].
     *
     * Eliminates race conditions between entering and exiting sheets' DisposableEffects.
     */
    fun onActiveSheetChanged(
        isNearbySheet: Boolean,
        transitTypeId: String? = null,
    ) {
        debounceLoadJob?.cancel()
        debounceLoadJob = null

        val previousTypeId = _uiState.value.selectedTransitTypeId
        val typeChanged = previousTypeId != transitTypeId
        isNearbySheetVisible = isNearbySheet

        if (!isNearbySheet) {
            updateRadiusJob?.cancel()
            computeNearbyStopsJob?.cancel()
        }

        _uiState.update { state ->
            val updatedSources = buildTransitSources(
                configs = state.transitConfigs,
                activeTypes = state.activeTransitTypes,
                selectedType = transitTypeId,
            )
            state.copy(
                selectedTransitTypeId = transitTypeId,
                transitSources = updatedSources,
                // Clear stale stops from a previous transit type immediately to prevent cross-type flash
                nearbyStops = if (isNearbySheet && typeChanged) emptyList() else state.nearbyStops,
            )
        }

        val vp = currentViewport
        if (transitTypeId != null) {
            if (vp != null && vp.zoom >= MIN_ZOOM_THRESHOLD && shouldFetchForType(transitTypeId, vp)) {
                loadTransitStopsData(vp, setOf(transitTypeId))
            } else if (isNearbySheet) {
                computeNearbyStopsForSheet()
            }
        } else {
            if (vp != null && vp.zoom >= MIN_ZOOM_THRESHOLD) {
                val typesNeedingFetch = _uiState.value.transitConfigs.map { it.typeId }
                    .filter { shouldFetchForType(it, vp) }
                if (typesNeedingFetch.isNotEmpty()) {
                    loadTransitStopsData(vp, typesNeedingFetch.toSet())
                } else if (isNearbySheet) {
                    computeNearbyStopsForSheet()
                }
            } else if (isNearbySheet) {
                computeNearbyStopsForSheet()
            }
        }
    }

    /**
     * Loads bus stops specifically for the current or specified viewport.
     */
    fun loadBusStopsData(viewport: TransitMapViewportInfo? = currentViewport) {
        val vp = viewport ?: return
        if (shouldFetchForType(TransitMode.BUS.name, vp)) {
            loadTransitStopsData(vp, setOf(TransitMode.BUS.name))
        } else if (isNearbySheetVisible) {
            computeNearbyStopsForSheet()
        }
    }

    /**
     * Loads subway / metro stations specifically for the current or specified viewport.
     */
    fun loadSubwayStationsData(viewport: TransitMapViewportInfo? = currentViewport) {
        val vp = viewport ?: return
        if (shouldFetchForType(TransitMode.SUBWAY.name, vp)) {
            loadTransitStopsData(vp, setOf(TransitMode.SUBWAY.name))
        } else if (isNearbySheetVisible) {
            computeNearbyStopsForSheet()
        }
    }

    /**
     * Loads bicycle rental stations specifically for the current or specified viewport.
     */
    fun loadBikeStationsData(viewport: TransitMapViewportInfo? = currentViewport) {
        val vp = viewport ?: return
        if (shouldFetchForType(TravelMode.BICYCLE_RENT.name, vp)) {
            loadTransitStopsData(vp, setOf(TravelMode.BICYCLE_RENT.name))
        } else if (isNearbySheetVisible) {
            computeNearbyStopsForSheet()
        }
    }

    /**
     * Loads all configured transit stop data for the current viewport.
     */
    fun loadAllTransitStopsData(viewport: TransitMapViewportInfo? = currentViewport) {
        val vp = viewport ?: return
        val typesNeedingFetch = _uiState.value.transitConfigs.map { it.typeId }
            .filter { shouldFetchForType(it, vp) }
        if (typesNeedingFetch.isNotEmpty()) {
            loadTransitStopsData(vp, typesNeedingFetch.toSet())
        }
    }

    fun onStopClicked(click: StopFeatureClick) {
        Timber.d("Stop clicked: ${click.stopId} (${click.name})")
        _uiState.update { it.copy(selectedStop = click) }
    }

    /**
     * Focuses the map camera on [stop] and opens the floating stop detail card.
     * Useful for transit modes that do not yet have a dedicated full detail screen (e.g. Bike).
     */
    fun focusOnNearbyStop(stop: NearbyStopItem) {
        _cameraFocusEvents.tryEmit(
            CameraFocusCommand(
                location = stop.location,
                zoom = 16.0,
                animate = true,
            )
        )
        _uiState.update {
            it.copy(
                selectedStop = StopFeatureClick(
                    stopId = stop.stopId,
                    typeId = stop.typeId,
                    name = stop.name,
                    point = Point.fromLngLat(stop.location.longitude, stop.location.latitude),
                    properties = stop.properties,
                )
            )
        }
    }

    /**
     * Handles clicking a nearby stop item: navigates to [TransitStopDetailRoute] for supported
     * transit modes (BUS, SUBWAY, RAIL, etc.), or focuses the stop on the map for Bike stations.
     */
    fun onNearbyStopSelected(stop: NearbyStopItem) {
        val transitMode = resolveTransitMode(stop.typeId)
        if (transitMode != null) {
            appNavigator.navigateTo(
                TransitStopDetailRoute(
                    stopId = stop.stopId,
                    transitMode = transitMode,
                )
            )
        } else {
            focusOnNearbyStop(stop)
        }
    }

    /**
     * Opens [TransitStopDetailRoute] from the floating stop detail card when supported.
     */
    fun navigateToSelectedStopDetail(stop: StopFeatureClick) {
        val transitMode = resolveTransitMode(stop.typeId) ?: return
        dismissStopDetail()
        appNavigator.navigateTo(
            TransitStopDetailRoute(
                stopId = stop.stopId,
                transitMode = transitMode,
            )
        )
    }

    private fun resolveTransitMode(typeId: String): TransitMode? = when (typeId.uppercase()) {
        TransitMode.BUS.name -> TransitMode.BUS
        TransitMode.SUBWAY.name, "METRO" -> TransitMode.SUBWAY
        TransitMode.RAIL.name, "TRAIN" -> TransitMode.RAIL
        TransitMode.TRAM.name -> TransitMode.TRAM
        TransitMode.FERRY.name -> TransitMode.FERRY
        TransitMode.MONORAIL.name -> TransitMode.MONORAIL
        else -> null
    }

    fun dismissStopDetail() {
        _uiState.update { it.copy(selectedStop = null) }
    }

    /**
     * Computes nearby stops around [targetLocation] or the current map center/user location
     * using pre-parsed in-memory stop lists.
     * Runs asynchronously on Dispatchers.Default to avoid blocking the Main UI thread.
     */
    fun computeNearbyStopsForSheet(targetLocation: LocationData? = null) {
        computeNearbyStopsJob?.cancel()
        val currentState = _uiState.value
        val visibleTypeIds = currentState.transitSources
            .filter { it.isVisible }
            .map { it.typeId }
        val parsedSnapshot = visibleTypeIds.associateWith { typeId ->
            cachedParsedStopsByType[typeId].orEmpty()
        }
        val center = targetLocation
            ?: currentMapCenter
            ?: currentState.currentLocation
            ?: return
        val radiusKm = currentState.selectedRadiusKm

        computeNearbyStopsJob = viewModelScope.launch(Dispatchers.Default) {
            val maxDistanceMeters = radiusKm * 1000.0
            val stops = parsedSnapshot.values
                .asSequence()
                .flatten()
                .map { parsed ->
                    val dist = parsed.location.distanceTo(center)
                    NearbyStopItem(
                        stopId = parsed.stopId,
                        name = parsed.name,
                        typeId = parsed.typeId,
                        location = parsed.location,
                        distanceMeters = dist,
                        routesOrInfo = parsed.routesOrInfo,
                        properties = parsed.properties,
                    )
                }
                .filter { it.distanceMeters <= maxDistanceMeters }
                .distinctBy { "${it.typeId}_${it.stopId}" }
                .sortedBy { it.distanceMeters }
                .toList()

            _uiState.update { it.copy(nearbyStops = stops) }
        }
    }

    /**
     * Updates selected radius in KM for nearby transit stops filter calculated from map zoom.
     * Immediately updates UI state (badge & info text) and debounces stops computation.
     */
    fun setSelectedRadiusKm(radiusKm: Double) {
        if (kotlin.math.abs(_uiState.value.selectedRadiusKm - radiusKm) < 0.005) return
        _uiState.update { it.copy(selectedRadiusKm = radiusKm) }
        if (isNearbySheetVisible) {
            scheduleNearbyRecompute()
        }
    }

    private fun parseStopsFromGeoJson(
        geoJsonStr: String,
        defaultTypeId: String,
    ): List<ParsedTransitStop> {
        if (geoJsonStr.isBlank()) return emptyList()
        val result = mutableListOf<ParsedTransitStop>()
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

                val propertiesObj = feature.optJSONObject("properties")
                val propertiesMap = mutableMapOf<String, String>()
                var stopId = feature.optString("id", "${defaultTypeId}_stop_$i")
                var name = ""
                var typeId = defaultTypeId
                var routesOrInfo = ""

                if (propertiesObj != null) {
                    val keys = propertiesObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        propertiesMap[key] = propertiesObj.optString(key, "")
                    }
                    if (!propertiesMap["stop_id"].isNullOrBlank()) {
                        stopId = propertiesMap["stop_id"]!!
                    }
                    name = propertiesMap["name"] ?: ""
                    typeId = propertiesMap["type"]?.ifBlank { defaultTypeId } ?: defaultTypeId
                    routesOrInfo = propertiesMap["routes"]
                        ?: propertiesMap["line"]
                        ?: propertiesMap["available_bikes"]?.let { "Xe khả dụng: $it" }
                        ?: ""
                }

                if (name.isBlank()) {
                    name = "Trạm $typeId ($stopId)"
                }

                result.add(
                    ParsedTransitStop(
                        stopId = stopId,
                        name = name,
                        typeId = typeId,
                        location = stopLocation,
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

    fun navigateToBusRouteSearch() {
        appNavigator.navigateTo(BusRouteSearchRoute)
    }

    fun navigateToRouteLocationPicker() {
        appNavigator.navigateTo(RoutePlannerContainerRoute)
    }
}

internal fun HomeContainerUiState.clearTransitLoading(): HomeContainerUiState =
    if (isLoadingTransit) copy(isLoadingTransit = false) else this
