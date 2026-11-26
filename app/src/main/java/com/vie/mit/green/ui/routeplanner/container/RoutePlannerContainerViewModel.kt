package com.vie.mit.green.ui.routeplanner.container

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.data.model.domain.map.AutosuggestPlace
import com.vie.mit.data.repository.geocode.GeocodeRepository
import com.vie.mit.data.repository.routeplanner.RoutePlannerRepository
import com.vie.mit.mapbox.model.LocationData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private data class SearchParams(
    val query: String,
    val latitude: Double,
    val longitude: Double,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class RoutePlannerContainerViewModel internal constructor(
    private val appNavigator: AppNavigator,
    private val geocodeRepository: GeocodeRepository,
    private val routePlannerRepository: RoutePlannerRepository,
    private val customScope: CoroutineScope?,
    private val searchDebounceMs: Long,
    private val reverseGeocodeDebounceMs: Long = 250L,
) : ViewModel() {

    companion object {
        const val CURRENT_LOCATION_LABEL = "Vị trí hiện tại"
    }

    @Inject
    constructor(
        appNavigator: AppNavigator,
        geocodeRepository: GeocodeRepository,
        routePlannerRepository: RoutePlannerRepository,
    ) : this(
        appNavigator = appNavigator,
        geocodeRepository = geocodeRepository,
        routePlannerRepository = routePlannerRepository,
        customScope = null,
        searchDebounceMs = 350L,
        reverseGeocodeDebounceMs = 250L,
    )

    private val scope: CoroutineScope
        get() = customScope ?: viewModelScope

    private val _uiState = MutableStateFlow(RoutePlannerContainerUiState())
    val uiState: StateFlow<RoutePlannerContainerUiState> = _uiState.asStateFlow()

    // Sự kiện yêu cầu camera Mapbox bay đến toạ độ chỉ định
    private val _cameraMoveEvent = MutableSharedFlow<LocationData>(extraBufferCapacity = 1)
    val cameraMoveEvent: SharedFlow<LocationData> = _cameraMoveEvent.asSharedFlow()

    private val searchParamsFlow = MutableStateFlow<SearchParams?>(null)
    private var searchJob: Job? = null
    private var reverseGeocodeJob: Job? = null
    private var flingSettleJob: Job? = null
    private var routeOptionsJob: Job? = null
    private var isUserDraggingMap: Boolean = false
    private var startTypedQuery: String? = null
    private var endTypedQuery: String? = null

    init {
        // Lắng nghe thay đổi từ khoá tìm kiếm với Debounce 350ms
        scope.launch {
            searchParamsFlow
                .debounce(searchDebounceMs.milliseconds)
                .distinctUntilChanged()
                .collect { params ->
                    if (params != null) {
                        executeSearch(params.query, params.latitude, params.longitude)
                    }
                }
        }
    }

    // ── Location Search & Suggestions ──────────────────────────────

    /**
     * Cập nhật chuỗi tìm kiếm khi người dùng nhập vào ô Start hoặc End.
     */
    fun onQueryChanged(
        field: LocationPickerField,
        newQuery: String,
    ) {
        cancelMapDragAndReverseGeocode()
        val mapCenter = _uiState.value.mapCenter
        if (newQuery.isBlank()) {
            setTypedQuery(field, null)
            cancelActiveSearch()
            _uiState.update { state ->
                if (field == LocationPickerField.START) {
                    state.copy(
                        focusedField = LocationPickerField.START,
                        startPoint = null,
                        startAddress = "",
                        startQuery = newQuery,
                        suggestions = emptyList(),
                        isLoadingSuggestions = false,
                        errorMessage = null,
                    )
                } else {
                    state.copy(
                        focusedField = LocationPickerField.END,
                        endPoint = null,
                        endAddress = "",
                        endQuery = newQuery,
                        suggestions = emptyList(),
                        isLoadingSuggestions = false,
                        errorMessage = null,
                    )
                }
            }
            return
        }

        val currentState = _uiState.value
        val keepExistingPoint = if (field == LocationPickerField.START) {
            newQuery == currentState.startAddress
        } else {
            newQuery == currentState.endAddress
        }
        setTypedQuery(field, if (keepExistingPoint) null else newQuery)

        _uiState.update { state ->
            if (field == LocationPickerField.START) {
                val keepPoint = newQuery == state.startAddress
                state.copy(
                    focusedField = LocationPickerField.START,
                    startPoint = if (keepPoint) state.startPoint else null,
                    startAddress = if (keepPoint) state.startAddress else "",
                    startQuery = newQuery,
                    errorMessage = null,
                )
            } else {
                val keepPoint = newQuery == state.endAddress
                state.copy(
                    focusedField = LocationPickerField.END,
                    endPoint = if (keepPoint) state.endPoint else null,
                    endAddress = if (keepPoint) state.endAddress else "",
                    endQuery = newQuery,
                    errorMessage = null,
                )
            }
        }
        searchParamsFlow.value = SearchParams(
            query = newQuery,
            latitude = mapCenter.latitude,
            longitude = mapCenter.longitude,
        )
    }

    /**
     * Xử lý khi người dùng chọn một địa điểm từ danh sách gợi ý.
     */
    fun onSelectSuggestion(place: AutosuggestPlace) {
        cancelActiveSearch()
        cancelMapDragAndReverseGeocode()
        val lat = place.latitude
        val lng = place.longitude
        val label = place.title
        val currentField = _uiState.value.focusedField
        setTypedQuery(currentField, null)

        if (currentField == LocationPickerField.START) {
            if (lat != null && lng != null) {
                updateStartLocation(LocationData(latitude = lat, longitude = lng), label)
            } else {
                _uiState.update {
                    it.copy(
                        startQuery = label,
                        suggestions = emptyList(),
                        isLoadingSuggestions = false,
                    )
                }
            }
        } else {
            if (lat != null && lng != null) {
                updateEndLocation(LocationData(latitude = lat, longitude = lng), label)
            } else {
                _uiState.update {
                    it.copy(
                        endQuery = label,
                        suggestions = emptyList(),
                        isLoadingSuggestions = false,
                    )
                }
            }
        }
    }

    /**
     * Dọn dẹp danh sách gợi ý (ví dụ sau khi đã chọn xong địa điểm).
     */
    fun clearSuggestions() {
        cancelActiveSearch()
        _uiState.update { it.copy(suggestions = emptyList(), isLoadingSuggestions = false) }
    }

    private fun cancelActiveSearch() {
        searchJob?.cancel()
        searchJob = null
        searchParamsFlow.value = null
    }

    /**
     * Thực hiện gọi API Autosuggest kết hợp Autocomplete với toạ độ tâm bản đồ.
     */
    private fun executeSearch(query: String, latitude: Double, longitude: Double) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(suggestions = emptyList(), isLoadingSuggestions = false) }
            return
        }

        searchJob = scope.launch {
            _uiState.update { it.copy(isLoadingSuggestions = true, errorMessage = null) }

            val result = geocodeRepository.getAutosuggest(
                query = query,
                latitude = latitude,
                longitude = longitude,
            )

            result.onSuccess { places ->
                _uiState.update {
                    it.copy(
                        suggestions = places,
                        isLoadingSuggestions = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                Timber.e(error, "getAutosuggest failed for query: $query, fallback to autocomplete")
                val fallbackResult = geocodeRepository.getAutocomplete(
                    query = query,
                    latitude = latitude,
                    longitude = longitude,
                )
                fallbackResult.onSuccess { autocompletePlaces ->
                    _uiState.update {
                        it.copy(
                            suggestions = autocompletePlaces,
                            isLoadingSuggestions = false,
                            errorMessage = null,
                        )
                    }
                }.onFailure { fallbackError ->
                    _uiState.update {
                        it.copy(
                            suggestions = emptyList(),
                            isLoadingSuggestions = false,
                            errorMessage = fallbackError.message,
                        )
                    }
                }
            }
        }
    }

    // ── Route Start / End Points ───────────────────────────────────

    /**
     * Cập nhật điểm xuất phát (Start) và đồng bộ text tìm kiếm.
     */
    fun updateStartLocation(
        point: LocationData,
        address: String,
        moveCamera: Boolean = true,
        autoSwitchFocus: Boolean = true,
    ) {
        cancelActiveSearch()
        cancelMapDragAndReverseGeocode()
        routeOptionsJob?.cancel()
        startTypedQuery = null
        _uiState.update { state ->
            state.copy(
                startPoint = point,
                startAddress = address,
                startQuery = address,
                focusedField = if (autoSwitchFocus && state.endPoint == null) {
                    LocationPickerField.END
                } else {
                    state.focusedField
                },
                suggestions = emptyList(),
                isLoadingSuggestions = false,
                routeOptions = emptyList(),
                isLoadingRoutes = false,
                routeOptionsError = null,
                selectedRouteOptionId = null,
            )
        }
        if (moveCamera) {
            _cameraMoveEvent.tryEmit(point)
        }
    }

    /**
     * Cập nhật điểm đến (End) và đồng bộ text tìm kiếm.
     */
    fun updateEndLocation(
        point: LocationData,
        address: String,
        moveCamera: Boolean = true,
    ) {
        cancelActiveSearch()
        cancelMapDragAndReverseGeocode()
        routeOptionsJob?.cancel()
        endTypedQuery = null
        _uiState.update { state ->
            state.copy(
                endPoint = point,
                endAddress = address,
                endQuery = address,
                suggestions = emptyList(),
                isLoadingSuggestions = false,
                routeOptions = emptyList(),
                isLoadingRoutes = false,
                routeOptionsError = null,
                selectedRouteOptionId = null,
            )
        }
        if (moveCamera) {
            _cameraMoveEvent.tryEmit(point)
        }
    }

    /**
     * Chọn nhanh vị trí GPS hiện tại cho trường đang focus (Start hoặc End).
     */
    fun useCurrentLocationForFocusedField() {
        val loc = lastKnownLocation ?: _uiState.value.currentLocation ?: return
        if (_uiState.value.focusedField == LocationPickerField.START) {
            updateStartLocation(loc, CURRENT_LOCATION_LABEL, moveCamera = true)
        } else {
            updateEndLocation(loc, CURRENT_LOCATION_LABEL, moveCamera = true)
        }
    }

    /**
     * Xóa điểm xuất phát và text tìm kiếm tương ứng.
     */
    fun clearStartLocation() {
        cancelActiveSearch()
        cancelMapDragAndReverseGeocode()
        routeOptionsJob?.cancel()
        startTypedQuery = null
        _uiState.update {
            it.copy(
                focusedField = LocationPickerField.START,
                startPoint = null,
                startAddress = "",
                startQuery = "",
                suggestions = emptyList(),
                isLoadingSuggestions = false,
                routeOptions = emptyList(),
                selectedRouteOptionId = null,
            )
        }
    }

    /**
     * Xóa điểm đến và text tìm kiếm tương ứng.
     */
    fun clearEndLocation() {
        cancelActiveSearch()
        cancelMapDragAndReverseGeocode()
        routeOptionsJob?.cancel()
        endTypedQuery = null
        _uiState.update {
            it.copy(
                focusedField = LocationPickerField.END,
                endPoint = null,
                endAddress = "",
                endQuery = "",
                suggestions = emptyList(),
                isLoadingSuggestions = false,
                routeOptions = emptyList(),
                selectedRouteOptionId = null,
            )
        }
    }

    /**
     * Hoán đổi vị trí xuất phát và vị trí đích (bao gồm cả text đang nhập).
     */
    fun swapLocations() {
        cancelActiveSearch()
        cancelMapDragAndReverseGeocode()
        routeOptionsJob?.cancel()
        val previousStartTypedQuery = startTypedQuery
        startTypedQuery = endTypedQuery
        endTypedQuery = previousStartTypedQuery
        _uiState.update { state ->
            state.copy(
                startPoint = state.endPoint,
                startAddress = state.endAddress,
                startQuery = state.endQuery,
                endPoint = state.startPoint,
                endAddress = state.startAddress,
                endQuery = state.startQuery,
                suggestions = emptyList(),
                isLoadingSuggestions = false,
                routeOptions = emptyList(),
                isLoadingRoutes = false,
                routeOptionsError = null,
                selectedRouteOptionId = null,
            )
        }
    }

    /**
     * Cập nhật trường nhập liệu đang được focus (Start hoặc End).
     */
    fun setFocusedField(field: LocationPickerField) {
        if (_uiState.value.focusedField != field) {
            cancelMapDragAndReverseGeocode()
        }
        _uiState.update { it.copy(focusedField = field) }
    }

    // ── Route Options (OTP2 Backend) ───────────────────────────────

    /**
     * Cập nhật thời điểm xuất phát do người dùng chọn (`null` = Khởi hành ngay).
     * Khi [reloadIfValidRoute] = true và đã có đủ Start/End, tự động tải lại danh sách phương án.
     */
    fun onSelectDepartureTime(
        departureTimeMillis: Long?,
        reloadIfValidRoute: Boolean = false,
    ) {
        routeOptionsJob?.cancel()
        _uiState.update { state ->
            state.copy(
                selectedDepartureTimeMillis = departureTimeMillis,
                routeOptions = emptyList(),
                isLoadingRoutes = false,
                routeOptionsError = null,
                selectedRouteOptionId = null,
            )
        }
        if (reloadIfValidRoute && _uiState.value.hasValidRoute) {
            loadRouteOptions(departureTimeMillis = departureTimeMillis)
        }
    }

    /**
     * Tải danh sách phương án di chuyển giữa điểm xuất phát và điểm đích từ repository.
     */
    fun loadRouteOptions(
        departureTimeMillis: Long? = _uiState.value.selectedDepartureTimeMillis,
        maxItineraries: Int = RoutePlannerRepository.DEFAULT_MAX_ITINERARIES,
    ) {
        val start = _uiState.value.startPoint
        val end = _uiState.value.endPoint
        if (start == null || end == null) {
            _uiState.update {
                it.copy(
                    routeOptions = emptyList(),
                    isLoadingRoutes = false,
                    routeOptionsError = null,
                    selectedRouteOptionId = null,
                )
            }
            return
        }

        // Nếu thời điểm người dùng chọn trước đó đã trôi qua, tự động chuyển về "Khởi hành ngay"
        val effectiveDepartureMillis = departureTimeMillis?.takeIf {
            it > System.currentTimeMillis() + 5_000L
        }

        routeOptionsJob?.cancel()
        routeOptionsJob = scope.launch {
            _uiState.update {
                it.copy(
                    isLoadingRoutes = true,
                    routeOptionsError = null,
                    selectedRouteFilter = RouteOptionFilter.ALL,
                )
            }

            routePlannerRepository.getRouteOptions(
                startLat = start.latitude,
                startLng = start.longitude,
                endLat = end.latitude,
                endLng = end.longitude,
                departureTimeMillis = effectiveDepartureMillis,
                maxItineraries = maxItineraries,
            ).onSuccess { options ->
                _uiState.update { state ->
                    val filtered = options.filter { state.selectedRouteFilter.matches(it) }
                    state.copy(
                        routeOptions = options,
                        isLoadingRoutes = false,
                        routeOptionsError = null,
                        selectedRouteOptionId = filtered.firstOrNull()?.id ?: options.firstOrNull()?.id,
                    )
                }
            }.onFailure { error ->
                Timber.e(error, "getRouteOptions failed")
                _uiState.update {
                    it.copy(
                        routeOptions = emptyList(),
                        isLoadingRoutes = false,
                        routeOptionsError = error.message ?: "Không thể tải phương án di chuyển",
                        selectedRouteOptionId = null,
                    )
                }
            }
        }
    }

    /**
     * Cập nhật bộ lọc phương tiện trong RouteOptionsSheet và giữ đồng bộ phương án đang chọn.
     */
    fun onSelectRouteFilter(filter: RouteOptionFilter) {
        _uiState.update { state ->
            val filtered = state.routeOptions.filter { filter.matches(it) }
            val nextSelectedId = if (filtered.any { it.id == state.selectedRouteOptionId }) {
                state.selectedRouteOptionId
            } else {
                filtered.firstOrNull()?.id
            }
            state.copy(
                selectedRouteFilter = filter,
                selectedRouteOptionId = nextSelectedId,
            )
        }
    }

    /**
     * Chọn một phương án di chuyển cụ thể trong RouteOptionsSheet.
     */
    fun onSelectRouteOption(optionId: String) {
        _uiState.update { it.copy(selectedRouteOptionId = optionId) }
    }

    // ── Map Camera & Center Picking ────────────────────────────────

    /**
     * Cập nhật toạ độ tâm bản đồ nền khi người dùng kéo/pan Mapbox.
     * - Khi người dùng đang giữ tay kéo (`isMapMoving == true`): chỉ cập nhật `mapCenter`,
     *   giữ nguyên `startPoint`/`endPoint` để chấm tròn trên map bám cố định theo vị trí cũ.
     * - Khi đã thả tay nhưng camera vẫn trôi theo quán tính fling (`!isMapMoving && isUserDraggingMap`):
     *   không đẩy chấm tròn theo từng frame mà lên lịch chốt điểm cuối sau khi dừng.
     */
    fun onMapCameraMoved(latitude: Double, longitude: Double) {
        val newCenter = LocationData(latitude = latitude, longitude = longitude)
        _uiState.update { it.copy(mapCenter = newCenter) }

        if (isUserDraggingMap && !_uiState.value.isMapMoving) {
            scheduleFlingSettle(newCenter)
        }
    }

    /**
     * Cập nhật trạng thái đang kéo bản đồ từ gesture của người dùng.
     * - Khi đang kéo (`isMoving == true`): giữ nguyên vị trí chấm tròn Start/End cũ trên bản đồ.
     * - Khi thả tay (`isMoving == false`): cập nhật chấm tròn theo tâm `CenterLocationMarker` hiện tại
     *   và lên lịch reverse geocode sau một khoảng debounce ngắn để bắt trọn quán tính fling nếu có.
     */
    fun onMapMoveStateChanged(isMoving: Boolean) {
        if (isMoving) {
            isUserDraggingMap = true
            cancelActiveSearch()
            flingSettleJob?.cancel()
            flingSettleJob = null
            reverseGeocodeJob?.cancel()
            reverseGeocodeJob = null
        }

        _uiState.update { it.copy(isMapMoving = isMoving) }

        if (!isMoving && isUserDraggingMap) {
            applyDraggedMapCenter(
                point = _uiState.value.mapCenter,
                triggerReverseGeocode = true,
                immediate = false,
            )
        }
    }

    /**
     * Được gọi khi camera Mapbox dừng hoàn toàn (hết drag & fling).
     * Nếu trước đó người dùng vừa kéo map, chốt toạ độ cuối cùng theo tâm `CenterLocationMarker`
     * và giải mã tên địa chỉ ngay.
     */
    fun onMapCameraIdle(latitude: Double, longitude: Double) {
        val finalCenter = LocationData(latitude = latitude, longitude = longitude)
        _uiState.update { it.copy(mapCenter = finalCenter) }

        if (isUserDraggingMap && !_uiState.value.isMapMoving) {
            isUserDraggingMap = false
            flingSettleJob?.cancel()
            flingSettleJob = null
            applyDraggedMapCenter(
                point = finalCenter,
                triggerReverseGeocode = true,
                immediate = true,
            )
        }
    }

    /**
     * Xác nhận lấy toạ độ tâm map hiện tại gán cho điểm đang active (Start hoặc End),
     * đồng thời gọi reverseGeocode để cập nhật tên địa chỉ.
     */
    fun onConfirmMapCenterLocation() {
        val currentCenter = _uiState.value.mapCenter
        val currentField = _uiState.value.focusedField
        val queryHint = getTypedQuery(currentField)
        val fallbackLabel = formatCoordinateLabel(currentCenter)

        if (currentField == LocationPickerField.START) {
            updateStartLocation(currentCenter, fallbackLabel, moveCamera = false)
        } else {
            updateEndLocation(currentCenter, fallbackLabel, moveCamera = false)
        }

        launchReverseGeocode(
            point = currentCenter,
            field = currentField,
            debounceMs = 0L,
            queryHint = queryHint,
        )
    }

    private fun applyDraggedMapCenter(
        point: LocationData,
        triggerReverseGeocode: Boolean,
        immediate: Boolean,
    ) {
        val currentField = _uiState.value.focusedField
        val coordinateLabel = formatCoordinateLabel(point)

        cancelActiveSearch()
        _uiState.update { state ->
            if (currentField == LocationPickerField.START) {
                state.copy(
                    startPoint = point,
                    startAddress = coordinateLabel,
                    startQuery = coordinateLabel,
                    suggestions = emptyList(),
                    isLoadingSuggestions = false,
                )
            } else {
                state.copy(
                    endPoint = point,
                    endAddress = coordinateLabel,
                    endQuery = coordinateLabel,
                    suggestions = emptyList(),
                    isLoadingSuggestions = false,
                )
            }
        }

        if (triggerReverseGeocode) {
            launchReverseGeocode(
                point = point,
                field = currentField,
                debounceMs = if (immediate) 0L else reverseGeocodeDebounceMs,
                queryHint = getTypedQuery(currentField),
            )
        }
    }

    private fun launchReverseGeocode(
        point: LocationData,
        field: LocationPickerField,
        debounceMs: Long,
        queryHint: String? = getTypedQuery(field),
    ) {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = scope.launch {
            if (debounceMs > 0L) {
                delay(debounceMs)
                isUserDraggingMap = false
            }
            geocodeRepository.reverseGeocode(
                latitude = point.latitude,
                longitude = point.longitude,
                queryHint = queryHint,
            ).onSuccess { resolvedLabel ->
                if (resolvedLabel.isBlank()) return@onSuccess
                _uiState.update { state ->
                    if (field == LocationPickerField.START && state.startPoint == point) {
                        state.copy(startAddress = resolvedLabel, startQuery = resolvedLabel)
                    } else if (field == LocationPickerField.END && state.endPoint == point) {
                        state.copy(endAddress = resolvedLabel, endQuery = resolvedLabel)
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun getTypedQuery(field: LocationPickerField): String? =
        if (field == LocationPickerField.START) startTypedQuery else endTypedQuery

    private fun setTypedQuery(field: LocationPickerField, query: String?) {
        if (field == LocationPickerField.START) {
            startTypedQuery = query
        } else {
            endTypedQuery = query
        }
    }

    private fun scheduleFlingSettle(point: LocationData) {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = null
        flingSettleJob?.cancel()
        flingSettleJob = scope.launch {
            if (reverseGeocodeDebounceMs > 0L) {
                delay(reverseGeocodeDebounceMs)
            }
            if (isUserDraggingMap && !_uiState.value.isMapMoving) {
                isUserDraggingMap = false
                applyDraggedMapCenter(
                    point = point,
                    triggerReverseGeocode = true,
                    immediate = true,
                )
            }
        }
    }

    private fun cancelMapDragAndReverseGeocode() {
        isUserDraggingMap = false
        flingSettleJob?.cancel()
        flingSettleJob = null
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = null
    }

    private fun formatCoordinateLabel(point: LocationData): String =
        String.format(Locale.US, "%.5f, %.5f", point.latitude, point.longitude)

    // ── User Location ──────────────────────────────────────────────

    private var lastKnownLocation: LocationData? = null
    private var hasLoadedInitialLocation: Boolean = false

    /**
     * Khi nhận được GPS lần đầu tiên:
     * - Lưu currentLocation.
     * - Nếu ô Điểm xuất phát đang trống: tự động điền "Vị trí hiện tại" và chuyển focus sang Điểm đến.
     */
    fun onLocationUpdated(location: LocationData) {
        lastKnownLocation = location

        if (!hasLoadedInitialLocation) {
            hasLoadedInitialLocation = true
            var shouldFocusCamera = false
            _uiState.update { state ->
                if (state.startPoint == null && state.startQuery.isBlank()) {
                    shouldFocusCamera = true
                    state.copy(
                        currentLocation = location,
                        mapCenter = location,
                        startPoint = location,
                        startAddress = CURRENT_LOCATION_LABEL,
                        startQuery = CURRENT_LOCATION_LABEL,
                        focusedField = LocationPickerField.END,
                    )
                } else {
                    state.copy(currentLocation = location)
                }
            }
            if (shouldFocusCamera) {
                _cameraMoveEvent.tryEmit(location)
            }
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
        cancelMapDragAndReverseGeocode()
        _uiState.update { it.copy(currentLocation = location) }
    }

    fun navigateBack() {
        appNavigator.navigateUp()
    }
}
