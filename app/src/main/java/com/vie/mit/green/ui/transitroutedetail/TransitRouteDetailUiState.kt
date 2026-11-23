package com.vie.mit.green.ui.transitroutedetail

import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.domain.map.RoundTrip
import com.vie.mit.data.model.domain.map.RouteTrip
import com.vie.mit.data.model.domain.map.ShapePoint
import com.vie.mit.data.model.domain.map.TransitStopMarker

/**
 * UI State for the Transit Route Detail screen.
 *
 * @param routeId Identifier of the transit route
 * @param typeId Type of transit (BUS, SUBWAY, etc.)
 * @param isLoading Whether route trips are currently being fetched
 * @param roundTrips Grouped round trips by service schedule
 * @param selectedServiceId Currently active service schedule identifier
 * @param selectedDirectionId Direction of travel: 0 (outbound / chiều đi), 1 (inbound / chiều về)
 * @param selectedTripId ID of the currently selected trip
 * @param shapes List of coordinate shape points for the selected trip
 * @param stops List of transit stop markers along the selected trip
 * @param isShapeLoading Whether shape points are currently loading
 * @param isStopsLoading Whether stop markers are currently loading
 * @param error Error message, if any
 */
data class TransitRouteDetailUiState(
    val routeId: String = "",
    val typeId: TransitMode = TransitMode.BUS,
    val isLoading: Boolean = false,
    val roundTrips: List<RoundTrip> = emptyList(),
    val selectedServiceId: String? = null,
    val selectedDirectionId: Int = 0,
    val selectedTripId: String? = null,
    val shapes: List<ShapePoint> = emptyList(),
    val stops: List<TransitStopMarker> = emptyList(),
    val isShapeLoading: Boolean = false,
    val isStopsLoading: Boolean = false,
    val error: String? = null,
) {
    /**
     * Currently active round trip matching [selectedServiceId].
     */
    val selectedRoundTrip: RoundTrip?
        get() = roundTrips.firstOrNull { it.serviceId == selectedServiceId }
            ?: roundTrips.firstOrNull()

    /**
     * Currently active trip matching [selectedTripId].
     */
    val selectedTrip: RouteTrip?
        get() = selectedRoundTrip?.getTrip(selectedDirectionId) ?: roundTrips.asSequence()
            .flatMap { sequenceOf(it.outboundTrip, it.inboundTrip) }.filterNotNull()
            .firstOrNull { it.tripId == selectedTripId }
}
