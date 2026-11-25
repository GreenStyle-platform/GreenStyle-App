package com.vie.mit.green.ui.routeplanner.container

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
    val selectedRouteFilter: String = "Tất cả",
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
}
