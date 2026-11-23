package com.vie.mit.data.network.domainmodel.map

import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.TransitStopDetailResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import com.vie.mit.data.network.api.map.MapApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.POST

class TransitStopApiModelTest {
    private val json = Json { ignoreUnknownKeys = true }

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
    fun transitStopDetailResponse_decodesBackendPayload() {
        val payload = """
            {
              "latitude": 20.9805232,
              "longitude": 105.7880024,
              "stopName": "Học viện công nghệ bưu chính viễn thông - Trần Phú (Hà Đông)",
              "stopId": "n444394950",
              "routes": [
                {
                  "routeId": "r12992622",
                  "routeShortName": "68",
                  "routeLongName": "Hà Đông (Bến xe Yên Nghĩa) ↔ Sân bay Nội Bài",
                  "routeDesc": null,
                  "routeType": 3,
                  "routeUrl": "https://jungle-bus.github.io/unroll/route.html?line=12992622",
                  "routeColor": null,
                  "routeTextColor": null,
                  "stopTimeResponses": [
                    {
                      "arrivalTime": "15:50:28",
                      "departureTime": "15:50:28",
                      "timepoint": 0
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val detail = json.decodeFromString(
            TransitStopDetailResponse.serializer(),
            payload,
        )

        assertEquals("n444394950", detail.resolvedId)
        assertEquals(
            "Học viện công nghệ bưu chính viễn thông - Trần Phú (Hà Đông)", detail.resolvedName
        )
        assertEquals(20.9805232, detail.latitude, 0.000001)
        assertEquals(105.7880024, detail.longitude, 0.000001)
        assertEquals(1, detail.routes.size)
        assertEquals("r12992622", detail.routes[0].routeId)
        assertEquals("68", detail.routes[0].routeShortName)
        assertEquals(1, detail.routes[0].stopTimeResponses.size)
        assertEquals("15:50:28", detail.routes[0].stopTimeResponses[0].arrivalTime)
    }

    @Test
    fun mapApi_usesTransitStopBoundsEndpoints() {
        val busMethod = MapApi::class.java.methods.single { it.name == "searchBusStopsInBounds" }
        val metroMethod =
            MapApi::class.java.methods.single { it.name == "searchMetroStopsInBounds" }
        val detailMethod = MapApi::class.java.methods.single { it.name == "getTransitStopDetail" }

        assertEquals(
            "transits/bus/search-in-bounds",
            requireNotNull(busMethod.getAnnotation(POST::class.java)).value,
        )
        assertEquals(
            "transits/subway/search-in-bounds",
            requireNotNull(metroMethod.getAnnotation(POST::class.java)).value,
        )
        assertEquals(
            "transits/{mode}/search-stop/{stopId}/routes",
            requireNotNull(detailMethod.getAnnotation(GET::class.java)).value,
        )
    }
}
