package com.vie.mit.data.network.domainmodel.map

import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.remote.response.map.RouteTripResponse
import com.vie.mit.data.model.remote.response.map.ShapePointResponse
import com.vie.mit.data.model.remote.response.map.TripStopResponse
import com.vie.mit.data.network.api.transit.TransitRouteApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.http.GET

class RouteTripAndShapeApiModelTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun routeTripResponse_decodesBackendPayload() {
        val payload = """
        [
          {
            "tripId": "r12997216_Mo,Tu,We,Th",
            "tripHeadsign": "Long Biên",
            "directionId": 0,
            "serviceId": "Mo,Tu,We,Th",
            "shapeId": null,
            "routeId": "r12997260",
            "routeShortName": "08B",
            "routeLongName": "Long Biên ↔ Vạn Phúc",
            "routeType": 3
          },
          {
            "tripId": "r12997216_Mo,Tu,We,Th",
            "tripHeadsign": "Vạn Phúc",
            "directionId": 1,
            "serviceId": "Mo,Tu,We,Th",
            "shapeId": null,
            "routeId": "r12997260",
            "routeShortName": "08B",
            "routeLongName": "Long Biên ↔ Vạn Phúc",
            "routeType": 3
          }
        ]
        """.trimIndent()
        val list = json.decodeFromString<List<RouteTripResponse>>(payload)
        assertEquals(2, list.size)
        val first = list[0]
        assertEquals("r12997216_Mo,Tu,We,Th", first.tripId)
        assertEquals("Long Biên", first.tripHeadsign)
        assertEquals(0, first.directionId)
        assertEquals("Mo,Tu,We,Th", first.serviceId)
        assertNull(first.shapeId)
        assertEquals("r12997260", first.routeId)
        assertEquals("08B", first.routeShortName)
        assertEquals("Long Biên ↔ Vạn Phúc", first.routeLongName)
        assertEquals(3, first.routeType)
        val domain = first.toDomain()
        assertEquals("r12997216_Mo,Tu,We,Th", domain.tripId)
        assertEquals("Long Biên", domain.tripHeadsign)
        assertEquals(0, domain.directionId)
        assertEquals("08B", domain.routeShortName)
    }

    @Test
    fun shapePointResponse_decodesBackendPayload() {
        val payload = """
        [
          {
            "shapeId": null,
            "shapePtSequence": null,
            "shapePtLat": 21.023641,
            "shapePtLon": 105.8607095,
            "shapeDistTraveled": null
          },
          {
            "shapeId": "shp_1",
            "shapePtSequence": 2,
            "shapePtLat": 21.0234764,
            "shapePtLon": 105.8607824,
            "shapeDistTraveled": 18.5
          }
        ]
        """.trimIndent()
        val list = json.decodeFromString<List<ShapePointResponse>>(payload)
        assertEquals(2, list.size)
        val first = list[0]
        assertNull(first.shapeId)
        assertNull(first.shapePtSequence)
        assertEquals(21.023641, first.shapePtLat!!, 0.000001)
        assertEquals(105.8607095, first.shapePtLon!!, 0.000001)
        assertNull(first.shapeDistTraveled)
        val domain = first.toDomain()
        assertNotNull(domain)
        assertEquals(21.023641, domain!!.latitude, 0.000001)
        assertEquals(105.8607095, domain.longitude, 0.000001)
        assertEquals(0, domain.sequence)
        assertNull(domain.shapeId)
        val secondDomain = list[1].toDomain()
        assertNotNull(secondDomain)
        assertEquals("shp_1", secondDomain!!.shapeId)
        assertEquals(2, secondDomain.sequence)
        assertEquals(18.5, secondDomain.distTraveled!!, 0.01)
    }

    @Test
    fun tripStopResponse_decodesBackendPayload() {
        val payload = """
        [
          {
            "id": "n738815312",
            "latitude": 21.0219618,
            "longitude": 105.8503463,
            "name": "58B Bà Triệu"
          },
          {
            "id": "n1008753034",
            "latitude": 21.0191699,
            "longitude": 105.8494294,
            "name": "92-94 Bà Triệu"
          }
        ]
        """.trimIndent()
        val list = json.decodeFromString<List<TripStopResponse>>(payload)
        assertEquals(2, list.size)
        val first = list[0]
        assertEquals("n738815312", first.id)
        assertEquals(21.0219618, first.latitude, 0.000001)
        assertEquals(105.8503463, first.longitude, 0.000001)
        assertEquals("58B Bà Triệu", first.name)
        val domain = first.toDomain()
        assertEquals("n738815312", domain.id)
        assertEquals("58B Bà Triệu", domain.name)
        assertEquals(21.0219618, domain.latitude, 0.000001)
        assertEquals(105.8503463, domain.longitude, 0.000001)
    }

    @Test
    fun transitRouteApi_endpointsAnnotationsAreCorrect() {
        val getRouteTripsMethod = TransitRouteApi::class.java.methods.single { it.name == "getRouteTrips" }
        val getTripShapeMethod = TransitRouteApi::class.java.methods.single { it.name == "getTripShape" }
        val getTripStopsMethod = TransitRouteApi::class.java.methods.single { it.name == "getTripStops" }
        val routeTripsAnnotation =
            requireNotNull(getRouteTripsMethod.getAnnotation(GET::class.java))
        assertEquals("transits/{mode}/search-trip-route", routeTripsAnnotation.value)
        val tripShapeAnnotation = requireNotNull(getTripShapeMethod.getAnnotation(GET::class.java))
        assertEquals("transits/{mode}/{tripId}/search-shape", tripShapeAnnotation.value)
        val tripStopsAnnotation = requireNotNull(getTripStopsMethod.getAnnotation(GET::class.java))
        assertEquals("transits/{mode}/trips/{tripId}/search-stops", tripStopsAnnotation.value)
    }
}
