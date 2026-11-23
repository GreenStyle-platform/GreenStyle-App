package com.vie.mit.green.ui.transitroutedetail

import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.domain.map.RoundTrip
import com.vie.mit.data.model.domain.map.RouteTrip
import com.vie.mit.data.model.domain.map.ShapePoint
import com.vie.mit.data.model.domain.map.TransitStopMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitRouteDetailUiStateTest {
    @Test
    fun defaultState_hasCorrectInitialValues() {
        val state = TransitRouteDetailUiState(
            routeId = "r12997260",
            typeId = TransitMode.BUS,
            isLoading = true,
        )

        assertEquals("r12997260", state.routeId)
        assertEquals(TransitMode.BUS, state.typeId)
        assertTrue(state.isLoading)
        assertTrue(state.roundTrips.isEmpty())
        assertNull(state.selectedTripId)
        assertNull(state.selectedServiceId)
        assertEquals(0, state.selectedDirectionId)
        assertTrue(state.shapes.isEmpty())
        assertTrue(state.stops.isEmpty())
        assertFalse(state.isShapeLoading)
        assertFalse(state.isStopsLoading)
        assertNull(state.error)
    }

    @Test
    fun roundTrip_getTripReturnsCorrectDirection() {
        val outbound = RouteTrip(
            tripId = "trip_out",
            tripHeadsign = "Long Biên",
            directionId = 0,
            serviceId = "Mo,Tu",
            routeId = "r123",
        )
        val inbound = RouteTrip(
            tripId = "trip_in",
            tripHeadsign = "Vạn Phúc",
            directionId = 1,
            serviceId = "Mo,Tu",
            routeId = "r123",
        )
        val roundTrip = RoundTrip(
            serviceId = "Mo,Tu",
            outboundTrip = outbound,
            inboundTrip = inbound,
            routeId = "r123",
            routeShortName = "08B",
            routeLongName = "Long Biên ↔ Vạn Phúc",
        )

        assertEquals("trip_out", roundTrip.getTrip(0)?.tripId)
        assertEquals("trip_in", roundTrip.getTrip(1)?.tripId)
        assertNull(roundTrip.getTrip(2))
    }

    @Test
    fun uiState_resolvesSelectedRoundTripAndSelectedTrip() {
        val trip1 = RouteTrip(
            tripId = "trip_weekday_0",
            tripHeadsign = "Long Biên",
            directionId = 0,
            serviceId = "Mo,Tu,We,Th",
        )
        val trip2 = RouteTrip(
            tripId = "trip_weekday_1",
            tripHeadsign = "Vạn Phúc",
            directionId = 1,
            serviceId = "Mo,Tu,We,Th",
        )
        val trip3 = RouteTrip(
            tripId = "trip_friday_0",
            tripHeadsign = "Long Biên",
            directionId = 0,
            serviceId = "Fr",
        )
        val rt1 = RoundTrip(serviceId = "Mo,Tu,We,Th", outboundTrip = trip1, inboundTrip = trip2)
        val rt2 = RoundTrip(serviceId = "Fr", outboundTrip = trip3, inboundTrip = null)
        val state = TransitRouteDetailUiState(
            routeId = "r12997260",
            roundTrips = listOf(rt1, rt2),
            selectedServiceId = "Mo,Tu,We,Th",
            selectedDirectionId = 1,
            selectedTripId = "trip_weekday_1",
            shapes = listOf(ShapePoint(latitude = 21.0, longitude = 105.8)),
            stops = listOf(
                TransitStopMarker(
                    id = "s1",
                    name = "Bến xe",
                    latitude = 21.0,
                    longitude = 105.8
                )
            ),
        )

        assertEquals("Mo,Tu,We,Th", state.selectedRoundTrip?.serviceId)
        val selectedTrip = state.selectedTrip
        assertNotNull(selectedTrip)
        assertEquals("trip_weekday_1", selectedTrip?.tripId)
        assertEquals("Vạn Phúc", selectedTrip?.tripHeadsign)
        assertEquals(1, state.shapes.size)
        assertEquals(1, state.stops.size)
    }
}
