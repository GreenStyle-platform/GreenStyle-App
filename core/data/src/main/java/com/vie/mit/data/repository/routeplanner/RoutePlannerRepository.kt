package com.vie.mit.data.repository.routeplanner

import com.vie.mit.data.model.domain.map.Location
import com.vie.mit.data.model.domain.map.PublicTransportSuggestion
import com.vie.mit.data.model.domain.map.RouteOption
import com.vie.mit.data.model.domain.map.RouteOptionTransportType
import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.mapper.toLocationRequest
import com.vie.mit.data.network.api.routeplanner.RoutePlannerApi
import com.vie.mit.data.network.util.handleApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutePlannerRepository @Inject constructor(
    private val routePlannerApi: RoutePlannerApi,
) {
    suspend fun getPublicTransportSuggestions(
        location: Location,
    ): Result<List<PublicTransportSuggestion>> = handleApiCall {
        routePlannerApi.getPublicTransportSuggestions(location.toLocationRequest())
            .map { it.toDomain() }
    }

    /**
     * Fetches multimodal route options between Start and End coordinates.
     *
     * TODO: Replace mock implementation with real backend route-planning API when available.
     */
    suspend fun getRouteOptions(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
    ): Result<List<RouteOption>> {
        val distanceKm = estimateDistanceKm(startLat, startLng, endLat, endLng)
        val busMinutes = (distanceKm * 4.2 + 8).toInt().coerceIn(10, 120)
        val metroMinutes = (distanceKm * 2.5 + 6).toInt().coerceIn(8, 90)
        val bikeMinutes = (distanceKm * 4.0 + 4).toInt().coerceIn(6, 120)
        val walkMinutes = (distanceKm * 12.0).toInt().coerceIn(5, 180)
        val walkMeters = (distanceKm * 1000).toInt().coerceAtLeast(150)

        return Result.success(
            listOf(
                RouteOption(
                    id = "opt_bus_fast",
                    title = "Xe buýt tuyến 01",
                    description = "Đi bộ 250m • Chuyến kế tiếp trong 5 phút",
                    durationMinutes = busMinutes,
                    costVnd = 7000,
                    walkDistanceMeters = 250,
                    transportType = RouteOptionTransportType.BUS,
                    tags = listOf("Nhanh nhất", "Tiết kiệm"),
                ),
                RouteOption(
                    id = "opt_metro_1",
                    title = "Tàu điện Metro Tuyến 1",
                    description = "Chuyển tuyến 1 lần • Giãn cách 8 phút",
                    durationMinutes = metroMinutes,
                    costVnd = 12000,
                    walkDistanceMeters = 400,
                    transportType = RouteOptionTransportType.SUBWAY,
                    tags = listOf("Đúng giờ", "Tránh kẹt xe"),
                ),
                RouteOption(
                    id = "opt_bike",
                    title = "Xe đạp công cộng TNGo",
                    description = "Thuê tại trạm gần nhất • Tuyến đường thoáng",
                    durationMinutes = bikeMinutes,
                    costVnd = 10000,
                    walkDistanceMeters = 100,
                    transportType = RouteOptionTransportType.BIKE,
                    tags = listOf("Thân thiện môi trường"),
                ),
                RouteOption(
                    id = "opt_walk",
                    title = "Đi bộ",
                    description = "Tuyến đường đi bộ an toàn có vỉa hè",
                    durationMinutes = walkMinutes,
                    costVnd = 0,
                    walkDistanceMeters = walkMeters,
                    transportType = RouteOptionTransportType.WALK,
                    tags = listOf("Tập luyện"),
                ),
            ),
        )
    }

    private fun estimateDistanceKm(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val sinLat = kotlin.math.sin(dLat / 2)
        val sinLng = kotlin.math.sin(dLng / 2)
        val a = sinLat * sinLat +
            kotlin.math.cos(Math.toRadians(lat1)) *
            kotlin.math.cos(Math.toRadians(lat2)) *
            sinLng * sinLng
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (earthRadiusKm * c).coerceAtLeast(0.5)
    }
}
