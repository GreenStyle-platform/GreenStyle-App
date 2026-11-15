package com.vie.mit.data.network.model.map

import com.vie.mit.data.network.api.map.MapApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.POST

class TransitStopApiModelTest {

    @Test
    fun boundsRequest_serializesWithBackendFieldNames() {
        val request = TransitStopBoundsRequest(
            minLat = 20.99,
            minLon = 105.78,
            maxLat = 21.05,
            maxLon = 105.90,
        )

        assertEquals(
            """{"minLat":20.99,"minLon":105.78,"maxLat":21.05,"maxLon":105.9}""",
            Json.encodeToString(TransitStopBoundsRequest.serializer(), request),
        )
    }

    @Test
    fun markerResponse_decodesWithoutLosingCoordinates() {
        val marker = Json.decodeFromString(
            TransitStopMarkerResponse.serializer(),
            """{"id":"n444394752","name":"Station name","latitude":20.9998526,"longitude":105.8151131}""",
        )

        assertEquals("n444394752", marker.id)
        assertEquals("Station name", marker.name)
        assertEquals(20.9998526, marker.latitude, 0.0)
        assertEquals(105.8151131, marker.longitude, 0.0)
    }

    @Test
    fun mapApi_usesTransitStopBoundsEndpoints() {
        val busMethod = MapApi::class.java.methods.single { it.name == "searchBusStopsInBounds" }
        val metroMethod = MapApi::class.java.methods.single { it.name == "searchMetroStopsInBounds" }

        assertEquals(
            "transit-stops/bus/search-in-bounds",
            busMethod.getAnnotation(POST::class.java).value,
        )
        assertEquals(
            "transit-stops/metro/search-in-bounds",
            metroMethod.getAnnotation(POST::class.java).value,
        )
    }
}
