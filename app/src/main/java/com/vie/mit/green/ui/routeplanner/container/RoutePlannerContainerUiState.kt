package com.vie.mit.green.ui.routeplanner.container

import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.domain.map.AutosuggestPlace
import com.vie.mit.data.model.domain.map.RouteOption
import com.vie.mit.mapbox.model.LocationData

/**
 * Trường nhập liệu đang được focus trong màn hình chọn địa điểm.
 */
enum class LocationPickerField {
    START,
    END,
}

/**
 * Bộ lọc phương tiện cho danh sách phương án di chuyển trong RouteOptionsSheet.
 */
enum class RouteOptionFilter(
    val transitMode: TransitMode? = null,
) {
    ALL(transitMode = null),
    BUS(transitMode = TransitMode.BUS),
    SUBWAY(transitMode = TransitMode.SUBWAY),
    BIKE(transitMode = TransitMode.BIKE),
    WALK(transitMode = TransitMode.WALK),
    ;

    fun matches(option: RouteOption): Boolean = when (this) {
        ALL -> true
        BUS -> TransitMode.BUS in option.transportTypes || TransitMode.TROLLEYBUS in option.transportTypes
        SUBWAY -> TransitMode.SUBWAY in option.transportTypes ||
            TransitMode.MONORAIL in option.transportTypes ||
            TransitMode.RAIL in option.transportTypes ||
            TransitMode.TRAM in option.transportTypes
        BIKE -> TransitMode.BIKE in option.transportTypes
        WALK -> option.transportTypes.isEmpty() || option.transportTypes.all { it == TransitMode.WALK }
    }
}

/**
 * Trạng thái UI dùng chung cho toàn bộ luồng RoutePlanner
 * (RoutePlannerContainerScreen, RouteLocationPickerSheet và RouteOptionsSheet).
 */
data class RoutePlannerContainerUiState(
    val startPoint: LocationData? = null,
    val startAddress: String = "",
    val startQuery: String = "",
    val endPoint: LocationData? = null,
    val endAddress: String = "",
    val endQuery: String = "",
    val mapCenter: LocationData = LocationData(latitude = 10.7769, longitude = 106.7009),
    val focusedField: LocationPickerField = LocationPickerField.START,
    val isMapMoving: Boolean = false,
    val currentLocation: LocationData? = null,
    val suggestions: List<AutosuggestPlace> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val errorMessage: String? = null,
    val selectedRouteFilter: RouteOptionFilter = RouteOptionFilter.ALL,
    val routeOptions: List<RouteOption> = emptyList(),
    val isLoadingRoutes: Boolean = false,
    val routeOptionsError: String? = null,
    val selectedRouteOptionId: String? = null,
) {
    /**
     * Xác định khi cả điểm xuất phát và điểm đích đều có toạ độ hợp lệ.
     */
    val hasValidRoute: Boolean
        get() = startPoint != null && endPoint != null

    /**
     * Danh sách phương án di chuyển đã áp dụng [selectedRouteFilter].
     */
    val filteredRouteOptions: List<RouteOption>
        get() = routeOptions.filter { selectedRouteFilter.matches(it) }

    /**
     * Phương án di chuyển đang được chọn trong danh sách hiện tại (dùng để đồng bộ polyline trên bản đồ).
     */
    val selectedRouteOption: RouteOption?
        get() = filteredRouteOptions.firstOrNull { it.id == selectedRouteOptionId }
            ?: filteredRouteOptions.firstOrNull()
}
