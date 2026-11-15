package com.vie.mit.data.repository.map

import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.request.map.TransitStopBoundsRequest
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.TransitStopMarkerResponse
import com.vie.mit.data.network.api.map.MapApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapRepositoryTest {
    @Test
    fun loadsBusAndMetroRequestsConcurrently() = runBlocking {
        val busStarted = CompletableDeferred<Unit>()
        val metroStarted = CompletableDeferred<Unit>()
        val releaseRequests = CompletableDeferred<Unit>()
        val repository = MapRepository(
            object : MapApi {
                override suspend fun getPublicTransportSuggestions(location: LocationRequest): List<PublicTransportSuggestionResponse> =
                    emptyList()

                override suspend fun searchBusStopsInBounds(bounds: TransitStopBoundsRequest): List<TransitStopMarkerResponse> {
                    busStarted.complete(Unit)
                    releaseRequests.await()
                    return emptyList()
                }

                override suspend fun searchMetroStopsInBounds(bounds: TransitStopBoundsRequest): List<TransitStopMarkerResponse> {
                    metroStarted.complete(Unit)
                    releaseRequests.await()
                    return emptyList()
                }
            },
        )
        val load = async { repository.getTransitStopsGeoJson(20.99, 105.78, 21.05, 105.90) }

        withTimeout(1_000) {
            busStarted.await()
            metroStarted.await()
        }
        releaseRequests.complete(Unit)

        assertTrue(load.await().isSuccess)
    }

    @Test
    fun mapsMarkersToGeoJsonAndKeepsUnsupportedConfiguredTypesEmpty() = runBlocking {
        val repository = MapRepository(
            FakeMapApi(
                busStops = listOf(
                    TransitStopMarkerResponse(
                        id = "bus-1",
                        name = "Bến xe",
                        latitude = 20.9998526,
                        longitude = 105.8151131,
                    ),
                ),
                metroStops = listOf(
                    TransitStopMarkerResponse(
                        id = "metro-1",
                        name = "Ga Cát Linh",
                        latitude = 21.0285,
                        longitude = 105.835,
                    ),
                ),
            ),
        )
        val result = repository.getTransitStopsGeoJson(20.99, 105.78, 21.05, 105.90)

        assertTrue(result.isSuccess)
        val load = result.getOrThrow()
        val sources = load.geoJsonByType
        assertEquals(setOf("BUS", "METRO", "TRAIN", "BIKE"), sources.keys)
        assertTrue(load.failedTypeIds.isEmpty())
        assertEquals(emptyList<String>(), featureCoordinates(sources.getValue("TRAIN")))
        assertEquals(emptyList<String>(), featureCoordinates(sources.getValue("BIKE")))
        val busFeature =
            Json.parseToJsonElement(sources.getValue("BUS")).jsonObject["features"]!!.jsonArray.first().jsonObject
        assertEquals("bus-1", busFeature["id"]!!.jsonPrimitive.content)
        assertEquals(
            listOf("105.8151131", "20.9998526"),
            busFeature["geometry"]!!.jsonObject["coordinates"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals("BUS", busFeature["properties"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals(
            "Bến xe",
            busFeature["properties"]!!.jsonObject["name"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun keepsSuccessfulModeWhenOtherModeFails() = runBlocking {
        val repository = MapRepository(
            FakeMapApi(
                busStops = listOf(TransitStopMarkerResponse("bus-1", "Bus", 21.0, 105.0)),
                metroFailure = IllegalStateException("Metro unavailable"),
            ),
        )
        val result = repository.getTransitStopsGeoJson(20.99, 105.78, 21.05, 105.90)

        assertTrue(result.isSuccess)
        val load = result.getOrThrow()
        val sources = load.geoJsonByType
        assertEquals(
            1,
            Json.parseToJsonElement(sources.getValue("BUS")).jsonObject["features"]!!.jsonArray.size
        )
        assertEquals(
            0,
            Json.parseToJsonElement(sources.getValue("METRO")).jsonObject["features"]!!.jsonArray.size
        )
        assertEquals(setOf("METRO"), load.failedTypeIds)
    }

    @Test
    fun failsWhenAllSupportedModeRequestsFail() = runBlocking {
        val repository = MapRepository(
            FakeMapApi(
                busFailure = IllegalStateException("Bus unavailable"),
                metroFailure = IllegalStateException("Metro unavailable"),
            ),
        )
        val result = repository.getTransitStopsGeoJson(20.99, 105.78, 21.05, 105.90)

        assertFalse(result.isSuccess)
    }

    @Test
    fun aggregatesAndDeduplicatesStopsFromSplitViewportRequests() = runBlocking {
        val repository = MapRepository(
            object : MapApi {
                override suspend fun getPublicTransportSuggestions(location: LocationRequest): List<PublicTransportSuggestionResponse> =
                    emptyList()

                override suspend fun searchBusStopsInBounds(bounds: TransitStopBoundsRequest): List<TransitStopMarkerResponse> =
                    if (bounds.minLon >= 0.0) {
                        listOf(TransitStopMarkerResponse("east", "East", 20.0, 179.0))
                    } else {
                        listOf(
                            TransitStopMarkerResponse("west", "West", 20.0, -179.0),
                            TransitStopMarkerResponse("east", "East", 20.0, 179.0),
                        )
                    }

                override suspend fun searchMetroStopsInBounds(bounds: TransitStopBoundsRequest): List<TransitStopMarkerResponse> =
                    emptyList()
            },
        )
        val result = repository.getTransitStopsGeoJson(
            listOf(
                TransitStopBoundsRequest(19.0, 178.0, 21.0, 180.0),
                TransitStopBoundsRequest(19.0, -180.0, 21.0, -178.0),
            ),
        )
        val features =
            Json.parseToJsonElement(result.getOrThrow().geoJsonByType.getValue("BUS")).jsonObject["features"]!!.jsonArray
        assertEquals(2, features.size)
        assertEquals(
            setOf("east", "west"),
            features.map { it.jsonObject["id"]!!.jsonPrimitive.content }.toSet(),
        )
    }

    private fun featureCoordinates(geoJson: String): List<String> =
        Json.parseToJsonElement(geoJson).jsonObject["features"]!!.jsonArray.flatMap { feature ->
            feature.jsonObject["geometry"]!!.jsonObject["coordinates"]!!.jsonArray.map { it.jsonPrimitive.content }
        }

    private class FakeMapApi(
        private val busStops: List<TransitStopMarkerResponse> = emptyList(),
        private val metroStops: List<TransitStopMarkerResponse> = emptyList(),
        private val busFailure: Throwable? = null,
        private val metroFailure: Throwable? = null,
    ) : MapApi {
        override suspend fun getPublicTransportSuggestions(location: LocationRequest): List<PublicTransportSuggestionResponse> =
            emptyList()

        override suspend fun searchBusStopsInBounds(bounds: TransitStopBoundsRequest): List<TransitStopMarkerResponse> {
            busFailure?.let { throw it }
            return busStops
        }

        override suspend fun searchMetroStopsInBounds(bounds: TransitStopBoundsRequest): List<TransitStopMarkerResponse> {
            metroFailure?.let { throw it }
            return metroStops
        }
    }
}
