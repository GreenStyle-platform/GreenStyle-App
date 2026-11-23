package com.vie.mit.green.ui.transitstopdetail

import com.vie.mit.data.model.common.map.PublicTransportType
import com.vie.mit.data.model.domain.map.StopArrival
import com.vie.mit.data.model.domain.map.VehicleArrival
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitStopDetailUiStateTest {

    @Test
    fun defaultState_hasCorrectInitialValues() {
        val state = TransitStopDetailUiState(
            stopId = "stop_123",
            type = PublicTransportType.BUS,
        )

        assertEquals("stop_123", state.stopId)
        assertEquals(PublicTransportType.BUS, state.type)
        assertEquals("BUS", state.typeId)
        assertTrue(state.isLoading)
        assertNull(state.error)
        assertEquals("", state.stopName)
        assertEquals(0.0, state.latitude, 0.0)
        assertEquals(0.0, state.longitude, 0.0)
        assertTrue(state.routeArrivals.isEmpty())
    }

    @Test
    fun updatedState_withStopDetail_containsAllRequiredFields() {
        val arrivals = listOf(
            StopArrival(
                stopId = "stop_123",
                routeId = "route_1",
                routeShortName = "01",
                routeLongName = "Bến Thành - Chợ Lớn",
                arrivals = listOf(VehicleArrival(etaMinutes = 5, arrivalTime = "12:30")),
            )
        )

        val state = TransitStopDetailUiState(
            stopId = "stop_123",
            type = PublicTransportType.SUBWAY,
            stopName = "Ga Bến Thành",
            latitude = 10.772,
            longitude = 106.698,
            address = "Quận 1, TP.HCM",
            routeArrivals = arrivals,
            isLoading = false,
        )

        assertFalse(state.isLoading)
        assertEquals("stop_123", state.stopId)
        assertEquals(PublicTransportType.SUBWAY, state.type)
        assertEquals("SUBWAY", state.typeId)
        assertEquals("Ga Bến Thành", state.stopName)
        assertEquals(10.772, state.latitude, 0.0)
        assertEquals(106.698, state.longitude, 0.0)
        assertEquals("Quận 1, TP.HCM", state.address)
        assertEquals(1, state.routeArrivals.size)
        assertEquals("01", state.routeArrivals.first().routeShortName)
        assertEquals(5, state.routeArrivals.first().arrivals.first().etaMinutes)
    }
}
