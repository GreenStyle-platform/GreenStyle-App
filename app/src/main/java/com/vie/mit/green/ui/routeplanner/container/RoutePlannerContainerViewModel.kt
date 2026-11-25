package com.vie.mit.green.ui.routeplanner.container

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vie.mit.common.navigation.AppNavigator
import com.vie.mit.data.model.domain.map.AutosuggestPlace
import com.vie.mit.data.repository.map.MapRepository
import com.vie.mit.mapbox.model.LocationData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
    private val mapRepository: MapRepository,
    private val customScope: CoroutineScope?,
    private val searchDebounceMs: Long,
) : ViewModel() {

    @Inject
    constructor(
        appNavigator: AppNavigator,
        mapRepository: MapRepository,
    ) : this(
        appNavigator = appNavigator,
        mapRepository = mapRepository,
        customScope = null,
        searchDebounceMs = 350L,
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
        val mapCenter = _uiState.value.mapCenter
        if (newQuery.isBlank()) {
            cancelActiveSearch()
            _uiState.update { state ->
                if (field == LocationPickerField.START) {
                    state.copy(
                        startQuery = newQuery,
                        suggestions = emptyList(),
                        isLoadingSuggestions = false,
                        errorMessage = null,
                    )
                } else {
                    state.copy(
                        endQuery = newQuery,
                        suggestions = emptyList(),
                        isLoadingSuggestions = false,
                        errorMessage = null,
                    )
                }
            }
            return
        }

        _uiState.update { state ->
            if (field == LocationPickerField.START) {
                state.copy(startQuery = newQuery, errorMessage = null)
            } else {
                state.copy(endQuery = newQuery, errorMessage = null)
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
        val lat = place.latitude
        val lng = place.longitude
        val label = place.title
        val currentField = _uiState.value.focusedField

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

            val result = mapRepository.getAutosuggest(
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
                val fallbackResult = mapRepository.getAutocomplete(
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
    fun updateStartLocation(point: LocationData, address: String) {
        cancelActiveSearch()
        _uiState.update { state ->
            state.copy(
                startPoint = point,
                startAddress = address,
                startQuery = address,
                focusedField = if (state.endPoint == null) LocationPickerField.END else state.focusedField,
                suggestions = emptyList(),
                isLoadingSuggestions = false,
            )
        }
        _cameraMoveEvent.tryEmit(point)
    }

    /**
     * Cập nhật điểm đến (End) và đồng bộ text tìm kiếm.
     */
    fun updateEndLocation(point: LocationData, address: String) {
        cancelActiveSearch()
        _uiState.update { state ->
            state.copy(
                endPoint = point,
                endAddress = address,
                endQuery = address,
                suggestions = emptyList(),
                isLoadingSuggestions = false,
            )
        }
        _cameraMoveEvent.tryEmit(point)
    }

    /**
     * Xóa điểm xuất phát và text tìm kiếm tương ứng.
     */
    fun clearStartLocation() {
        cancelActiveSearch()
        _uiState.update {
            it.copy(
                startPoint = null,
                startAddress = "",
                startQuery = "",
                suggestions = emptyList(),
                isLoadingSuggestions = false,
            )
        }
    }

    /**
     * Xóa điểm đến và text tìm kiếm tương ứng.
     */
    fun clearEndLocation() {
        cancelActiveSearch()
        _uiState.update {
            it.copy(
                endPoint = null,
                endAddress = "",
                endQuery = "",
                suggestions = emptyList(),
                isLoadingSuggestions = false,
            )
        }
    }

    /**
     * Hoán đổi vị trí xuất phát và vị trí đích (bao gồm cả text đang nhập).
     */
    fun swapLocations() {
        cancelActiveSearch()
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
            )
        }
    }

    /**
     * Cập nhật trường nhập liệu đang được focus (Start hoặc End).
     */
    fun setFocusedField(field: LocationPickerField) {
        _uiState.update { it.copy(focusedField = field) }
    }

    /**
     * Cập nhật bộ lọc phương tiện trong RouteOptionsSheet.
     */
    fun onSelectRouteFilter(filter: String) {
        _uiState.update { it.copy(selectedRouteFilter = filter) }
    }

    /**
     * Cập nhật toạ độ tâm bản đồ nền khi người dùng kéo/pan Mapbox.
     */
    fun onMapCameraMoved(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(mapCenter = LocationData(latitude = latitude, longitude = longitude))
        }
    }

    /**
     * Xác nhận lấy toạ độ tâm map hiện tại gán cho điểm đang active (Start hoặc End).
     */
    fun onConfirmMapCenterLocation() {
        val currentCenter = _uiState.value.mapCenter
        val currentField = _uiState.value.focusedField
        val label = "Vị trí đã chọn (${String.format("%.4f, %.4f", currentCenter.latitude, currentCenter.longitude)})"

        if (currentField == LocationPickerField.START) {
            updateStartLocation(currentCenter, label)
        } else {
            updateEndLocation(currentCenter, label)
        }
    }

    // ── User Location ──────────────────────────────────────────────

    private var lastKnownLocation: LocationData? = null
    private var hasLoadedInitialLocation: Boolean = false

    /**
     * Called when the user's GPS location is first acquired or updated.
     * Caches the latest location; only updates UI State on the very first fix.
     */
    fun onLocationUpdated(location: LocationData) {
        lastKnownLocation = location

        if (!hasLoadedInitialLocation) {
            hasLoadedInitialLocation = true
            _uiState.update { it.copy(currentLocation = location) }
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
        _uiState.update { it.copy(currentLocation = location) }
    }

    fun navigateBack() {
        appNavigator.navigateUp()
    }
}
