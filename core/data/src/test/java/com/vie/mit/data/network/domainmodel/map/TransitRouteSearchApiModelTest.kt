package com.vie.mit.data.network.domainmodel.map

import com.vie.mit.data.model.common.map.TransitMode
import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.remote.request.map.LocationRequest
import com.vie.mit.data.model.remote.request.map.TransitRouteSearchRequest
import com.vie.mit.data.model.remote.response.map.PublicTransportSuggestionResponse
import com.vie.mit.data.model.remote.response.map.TransitRouteSearchResponse
import com.vie.mit.data.network.api.routeplanner.RoutePlannerApi
import com.vie.mit.data.repository.routeplanner.RoutePlannerRepository
import com.vie.mit.data.util.PolylineDecoder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.POST

class TransitRouteSearchApiModelTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun transitRouteSearchRequest_serializesWithExpectedBackendFieldNames() {
        val request = TransitRouteSearchRequest(
            originLatitude = 21.028170064544017,
            originLongitude = 105.74122052518302,
            destinationLatitude = 20.978153621560082,
            destinationLongitude = 105.78475726681633,
            departureTime = "2026-11-27T08:46:57.176Z",
            maxItineraries = 5,
        )

        val encoded = Json.encodeToString(TransitRouteSearchRequest.serializer(), request)

        assertTrue(encoded.contains(""""originLatitude":21.028170064544017"""))
        assertTrue(encoded.contains(""""originLongitude":105.74122052518302"""))
        assertTrue(encoded.contains(""""destinationLatitude":20.978153621560082"""))
        assertTrue(encoded.contains(""""destinationLongitude":105.78475726681633"""))
        assertTrue(encoded.contains(""""departureTime":"2026-11-27T08:46:57.176Z""""))
        assertTrue(encoded.contains(""""maxItineraries":5"""))
    }

    @Test
    fun routePlannerApi_usesTransitRoutesSearchPostEndpoint() {
        val method = RoutePlannerApi::class.java.methods.single { it.name == "searchTransitRoutes" }
        val annotation = requireNotNull(method.getAnnotation(POST::class.java))
        assertEquals("transit-routes/search", annotation.value)
    }

    @Test
    fun transitMode_fromOtpMode_mapsAllOtp2Modes() {
        assertEquals(TransitMode.WALK, TransitMode.fromOtpMode("WALK"))
        assertEquals(TransitMode.BUS, TransitMode.fromOtpMode("BUS"))
        assertEquals(TransitMode.BUS, TransitMode.fromOtpMode("COACH"))
        assertEquals(TransitMode.SUBWAY, TransitMode.fromOtpMode("SUBWAY"))
        assertEquals(TransitMode.SUBWAY, TransitMode.fromOtpMode("METRO"))
        assertEquals(TransitMode.BIKE, TransitMode.fromOtpMode("BICYCLE"))
        assertEquals(TransitMode.RAIL, TransitMode.fromOtpMode("RAIL"))
        assertEquals(TransitMode.TRAM, TransitMode.fromOtpMode("TRAM"))
        assertEquals(TransitMode.FERRY, TransitMode.fromOtpMode("FERRY"))
        assertEquals(TransitMode.CABLE_TRAM, TransitMode.fromOtpMode("CABLE_CAR"))
        assertEquals(TransitMode.AERIAL_LIFT, TransitMode.fromOtpMode("GONDOLA"))
        assertEquals(TransitMode.MONORAIL, TransitMode.fromOtpMode("MONORAIL"))
        assertEquals(TransitMode.TROLLEYBUS, TransitMode.fromOtpMode("TROLLEYBUS"))
        assertEquals(TransitMode.WALK, TransitMode.fromOtpMode(null))
    }

    @Test
    fun polylineDecoder_decodesOtp2GeometriesCloseToFromAndToCoordinates() {
        // Subway 2A leg geometry from real OTP2 payload
        val subwayPoints = PolylineDecoder.decode(
            "{`c_Co|wdSn@dAv@pAvBfDpLhRrLpRzIhNT^T`@NTLNBFPRx@nADFv@jAJNz@lARXdCnDrAfBh@p@h@t@dDlEtFpHxAlBpGxIfAzAJL",
        )
        assertTrue(subwayPoints.size > 10)
        assertEquals(20.9923, subwayPoints.first().latitude, 0.002)
        assertEquals(105.8044, subwayPoints.first().longitude, 0.002)
        assertEquals(20.9777, subwayPoints.last().latitude, 0.002)
        assertEquals(105.7848, subwayPoints.last().longitude, 0.002)

        // Final walk leg geometry from Văn Quán -> Destination
        val walkPoints = PolylineDecoder.decode("{e`_CwatdSSRIMk@u@[Z")
        assertTrue(walkPoints.size >= 2)
        assertEquals(20.9777, walkPoints.first().latitude, 0.002)
        assertEquals(105.7848, walkPoints.first().longitude, 0.002)
        assertEquals(20.9782, walkPoints.last().latitude, 0.002)
        assertEquals(105.7848, walkPoints.last().longitude, 0.002)
    }

    @Test
    fun transitRouteSearchResponse_decodesAndMapsRealOtp2PayloadToDomain() {
        val response = json.decodeFromString(
            TransitRouteSearchResponse.serializer(),
            SAMPLE_OTP2_PAYLOAD,
        )

        assertEquals(2, response.itineraries.size)
        val options = response.toDomain()
        assertEquals(2, options.size)

        val multimodal = options[0]
        assertEquals("otp-1", multimodal.id)
        assertEquals(5222, multimodal.durationSeconds)
        assertEquals(87, multimodal.durationMinutes)
        assertEquals(2958, multimodal.walkSeconds)
        assertEquals(49, multimodal.walkMinutes)
        assertEquals(1, multimodal.transfers)
        assertEquals(1795769562000L, multimodal.startTimeMillis)
        assertEquals(1795774784000L, multimodal.endTimeMillis)
        assertNull(multimodal.costVnd)
        assertEquals(TransitMode.BUS, multimodal.transportType)
        assertEquals(setOf(TransitMode.WALK, TransitMode.BUS, TransitMode.SUBWAY), multimodal.transportTypes)
        assertEquals("Xe buýt 29 → Metro 2A", multimodal.title)
        assertEquals("1 lần chuyển tuyến • Đi bộ 49 phút", multimodal.description)
        assertEquals(listOf("Nhanh nhất", "Ít đi bộ nhất"), multimodal.tags)
        assertEquals(5, multimodal.legs.size)
        assertEquals("29", multimodal.legs[1].routeShortName)
        assertEquals("2A", multimodal.legs[3].routeShortName)
        assertTrue(multimodal.legs.all { it.points.size >= 2 })

        val singleBus = options[1]
        assertEquals("otp-3", singleBus.id)
        assertEquals(6535, singleBus.durationSeconds)
        assertEquals(109, singleBus.durationMinutes)
        assertEquals(0, singleBus.transfers)
        assertEquals(setOf(TransitMode.WALK, TransitMode.BUS), singleBus.transportTypes)
        assertEquals("Xe buýt 29", singleBus.title)
        assertEquals(listOf("Đi thẳng"), singleBus.tags)
        assertEquals(3, singleBus.legs.size)
    }

    @Test
    fun routePlannerRepository_getRouteOptions_sendsFormattedRequestAndMapsResponse() = runBlocking {
        var capturedRequest: TransitRouteSearchRequest? = null
        val fakeApi = object : RoutePlannerApi {
            override suspend fun getPublicTransportSuggestions(
                location: LocationRequest,
            ): List<PublicTransportSuggestionResponse> = emptyList()

            override suspend fun searchTransitRoutes(
                request: TransitRouteSearchRequest,
            ): TransitRouteSearchResponse {
                capturedRequest = request
                return json.decodeFromString(TransitRouteSearchResponse.serializer(), SAMPLE_OTP2_PAYLOAD)
            }
        }
        val repository = RoutePlannerRepository(fakeApi)

        val result = repository.getRouteOptions(
            startLat = 21.028170064544017,
            startLng = 105.74122052518302,
            endLat = 20.978153621560082,
            endLng = 105.78475726681633,
            departureTimeMillis = 1795769217176L,
            maxItineraries = 5,
        )

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        val req = requireNotNull(capturedRequest)
        assertEquals(21.028170064544017, req.originLatitude, 0.0)
        assertEquals(105.74122052518302, req.originLongitude, 0.0)
        assertEquals(20.978153621560082, req.destinationLatitude, 0.0)
        assertEquals(105.78475726681633, req.destinationLongitude, 0.0)
        assertEquals("2026-11-27T08:46:57.176Z", req.departureTime)
        assertEquals(5, req.maxItineraries)
    }

    @Test
    fun routePlannerRepository_getRouteOptions_propagatesFailure() = runBlocking {
        val fakeApi = object : RoutePlannerApi {
            override suspend fun getPublicTransportSuggestions(
                location: LocationRequest,
            ): List<PublicTransportSuggestionResponse> = emptyList()

            override suspend fun searchTransitRoutes(
                request: TransitRouteSearchRequest,
            ): TransitRouteSearchResponse {
                throw IllegalStateException("OTP unavailable")
            }
        }
        val repository = RoutePlannerRepository(fakeApi)

        val result = repository.getRouteOptions(21.0, 105.7, 20.9, 105.8)

        assertTrue(result.isFailure)
    }

    @Test
    fun parseUtcIsoTimestampMillis_handlesNanosecondsFromBackendErrorPayload() {
        val parsed = RoutePlannerRepository.parseUtcIsoTimestampMillis(
            "2026-11-26T15:48:38.149436491Z",
        )
        assertEquals(
            "2026-11-26T15:48:38.149Z",
            RoutePlannerRepository.formatUtcIsoTimestamp(requireNotNull(parsed)),
        )
    }

    @Test
    fun routePlannerRepository_departNow_appliesForwardBufferAndRetriesWhenServerClockIsAhead() = runBlocking {
        val capturedRequests = mutableListOf<TransitRouteSearchRequest>()
        val futureServerMillis = System.currentTimeMillis() + 10 * 60_000L
        val futureServerTimestamp = RoutePlannerRepository.formatUtcIsoTimestamp(futureServerMillis)
            .replace("Z", "436491Z")

        val fakeApi = object : RoutePlannerApi {
            override suspend fun getPublicTransportSuggestions(
                location: LocationRequest,
            ): List<PublicTransportSuggestionResponse> = emptyList()

            override suspend fun searchTransitRoutes(
                request: TransitRouteSearchRequest,
            ): TransitRouteSearchResponse {
                capturedRequests.add(request)
                if (capturedRequests.size == 1) {
                    val errorJson = """
                        {
                          "message": "Thời điểm khởi hành phải là hiện tại hoặc trong tương lai",
                          "status": "error",
                          "timestamp": "$futureServerTimestamp"
                        }
                    """.trimIndent()
                    throw HttpException(
                        Response.error<TransitRouteSearchResponse>(
                            400,
                            errorJson.toResponseBody("application/json".toMediaType()),
                        ),
                    )
                }
                return json.decodeFromString(TransitRouteSearchResponse.serializer(), SAMPLE_OTP2_PAYLOAD)
            }
        }

        val repository = RoutePlannerRepository(fakeApi)
        val beforeCallMillis = System.currentTimeMillis()
        val result = repository.getRouteOptions(21.0075426, 105.7772323, 20.9804523, 105.7869506)

        assertTrue(result.isSuccess)
        assertEquals(2, capturedRequests.size)

        val firstDepartureMillis = requireNotNull(
            RoutePlannerRepository.parseUtcIsoTimestampMillis(capturedRequests[0].departureTime),
        )
        assertTrue(
            "Expected first departureTime to include forward buffer",
            firstDepartureMillis >= beforeCallMillis + RoutePlannerRepository.DEFAULT_DEPART_NOW_BUFFER_MS - 1_000L,
        )

        val secondDepartureMillis = requireNotNull(
            RoutePlannerRepository.parseUtcIsoTimestampMillis(capturedRequests[1].departureTime),
        )
        assertEquals(
            futureServerMillis + RoutePlannerRepository.RETRY_DEPART_NOW_BUFFER_MS,
            secondDepartureMillis,
        )
    }

    companion object {
        private val SAMPLE_OTP2_PAYLOAD = """
            {
              "itineraries": [
                {
                  "durationSeconds": 5222,
                  "endTime": "1795774784000",
                  "id": "otp-1",
                  "legs": [
                    {
                      "durationSeconds": 2325,
                      "endTime": "1795771887000",
                      "from": {
                        "arrivalTime": "1795769562000",
                        "departureTime": "1795769562000",
                        "id": null,
                        "latitude": 21.0281701,
                        "longitude": 105.7412205,
                        "name": "Origin"
                      },
                      "geometry": "uaj_CkqkdSZtAg@JOBSD_@Fk@ViAlASq@]yAAOIwACa@IMmAL_AVIBq@TOi@[H[qAw@Rc@NM_@_@_AWs@uDJC",
                      "headsign": null,
                      "mode": "WALK",
                      "routeId": null,
                      "routeLongName": null,
                      "routeShortName": null,
                      "startTime": "1795769562000",
                      "to": {
                        "arrivalTime": "1795771887000",
                        "departureTime": "1795771887000",
                        "id": "1:n8912013119",
                        "latitude": 21.04526,
                        "longitude": 105.7514727,
                        "name": "150-152 tổ 3 đường 32 (đối diện Công ty Vật liệu & Bưu điện)"
                      }
                    },
                    {
                      "durationSeconds": 1705,
                      "endTime": "1795773592000",
                      "from": {
                        "arrivalTime": "1795771887000",
                        "departureTime": "1795771887000",
                        "id": "1:n8912013119",
                        "latitude": 21.04526,
                        "longitude": 105.7514727,
                        "name": "150-152 tổ 3 đường 32 (đối diện Công ty Vật liệu & Bưu điện)"
                      },
                      "geometry": "_lm_CwqmdSLe@\\iAbAsDFSRq@tBuH\\oAPq@J[",
                      "headsign": "Bến xe Giáp Bát",
                      "mode": "BUS",
                      "routeId": "1:r12928808",
                      "routeLongName": "Bến xe Giáp Bát ↔ Tân Lập",
                      "routeShortName": "29",
                      "startTime": "1795771887000",
                      "to": {
                        "arrivalTime": "1795773592000",
                        "departureTime": "1795773592000",
                        "id": "1:n444394999",
                        "latitude": 20.9934876,
                        "longitude": 105.8056706,
                        "name": "386 Nguyễn Trãi - Cục Sở hữu Trí tuệ"
                      }
                    },
                    {
                      "durationSeconds": 459,
                      "endTime": "1795774051000",
                      "from": {
                        "arrivalTime": "1795773592000",
                        "departureTime": "1795773592000",
                        "id": "1:n444394999",
                        "latitude": 20.9934876,
                        "longitude": 105.8056706,
                        "name": "386 Nguyễn Trãi - Cục Sở hữu Trí tuệ"
                      },
                      "geometry": "ghc_CmdxdSCBHLv@pAv@jAp@dAP\\FJFJZl@?@HN@D?L?NPAOo@GITQNTJIu@mABA",
                      "headsign": null,
                      "mode": "WALK",
                      "routeId": null,
                      "routeLongName": null,
                      "routeShortName": null,
                      "startTime": "1795773592000",
                      "to": {
                        "arrivalTime": "1795774051000",
                        "departureTime": "1795774051000",
                        "id": "2:n6819506965",
                        "latitude": 20.9923086,
                        "longitude": 105.8044088,
                        "name": "Vành Đai 3"
                      }
                    },
                    {
                      "durationSeconds": 333,
                      "endTime": "1795774610000",
                      "from": {
                        "arrivalTime": "1795774277000",
                        "departureTime": "1795774277000",
                        "id": "2:n6819506965",
                        "latitude": 20.9923086,
                        "longitude": 105.8044088,
                        "name": "Vành Đai 3"
                      },
                      "geometry": "{`c_Co|wdSn@dAv@pAvBfDpLhRrLpRzIhNT^T`@NTLNBFPRx@nADFv@jAJNz@lARXdCnDrAfBh@p@h@t@dDlEtFpHxAlBpGxIfAzAJL",
                      "headsign": "Yên Nghĩa",
                      "mode": "SUBWAY",
                      "routeId": "2:metro_2a",
                      "routeLongName": "Cát Linh - Hà Đông",
                      "routeShortName": "2A",
                      "startTime": "1795774277000",
                      "to": {
                        "arrivalTime": "1795774610000",
                        "departureTime": "1795774610000",
                        "id": "2:n6544472083",
                        "latitude": 20.9777421,
                        "longitude": 105.784762,
                        "name": "Văn Quán"
                      }
                    },
                    {
                      "durationSeconds": 174,
                      "endTime": "1795774784000",
                      "from": {
                        "arrivalTime": "1795774610000",
                        "departureTime": "1795774610000",
                        "id": "2:n6544472083",
                        "latitude": 20.9777421,
                        "longitude": 105.784762,
                        "name": "Văn Quán"
                      },
                      "geometry": "{e`_CwatdSSRIMk@u@[Z",
                      "headsign": null,
                      "mode": "WALK",
                      "routeId": null,
                      "routeLongName": null,
                      "routeShortName": null,
                      "startTime": "1795774610000",
                      "to": {
                        "arrivalTime": "1795774784000",
                        "departureTime": "1795774784000",
                        "id": null,
                        "latitude": 20.9781536,
                        "longitude": 105.7847573,
                        "name": "Destination"
                      }
                    }
                  ],
                  "startTime": "1795769562000",
                  "transfers": 1,
                  "walkSeconds": 2958
                },
                {
                  "durationSeconds": 6535,
                  "endTime": "1795776097000",
                  "id": "otp-3",
                  "legs": [
                    {
                      "durationSeconds": 2325,
                      "endTime": "1795771887000",
                      "from": {
                        "latitude": 21.0281701,
                        "longitude": 105.7412205,
                        "name": "Origin"
                      },
                      "geometry": "uaj_CkqkdSZtAg@JOBSD_@",
                      "mode": "WALK",
                      "startTime": "1795769562000",
                      "to": {
                        "id": "1:n8912013119",
                        "latitude": 21.04526,
                        "longitude": 105.7514727,
                        "name": "150-152 tổ 3 đường 32"
                      }
                    },
                    {
                      "durationSeconds": 1705,
                      "endTime": "1795773592000",
                      "from": {
                        "id": "1:n8912013119",
                        "latitude": 21.04526,
                        "longitude": 105.7514727,
                        "name": "150-152 tổ 3 đường 32"
                      },
                      "geometry": "_lm_CwqmdSLe@\\iAbAsDFSRq@",
                      "headsign": "Bến xe Giáp Bát",
                      "mode": "BUS",
                      "routeId": "1:r12928808",
                      "routeLongName": "Bến xe Giáp Bát ↔ Tân Lập",
                      "routeShortName": "29",
                      "startTime": "1795771887000",
                      "to": {
                        "id": "1:n444394999",
                        "latitude": 20.9934876,
                        "longitude": 105.8056706,
                        "name": "386 Nguyễn Trãi - Cục Sở hữu Trí tuệ"
                      }
                    },
                    {
                      "durationSeconds": 2505,
                      "endTime": "1795776097000",
                      "from": {
                        "id": "1:n444394999",
                        "latitude": 20.9934876,
                        "longitude": 105.8056706,
                        "name": "386 Nguyễn Trãi - Cục Sở hữu Trí tuệ"
                      },
                      "geometry": "{e`_CwatdSSRIMk@u@[Z",
                      "mode": "WALK",
                      "startTime": "1795773592000",
                      "to": {
                        "latitude": 20.9781536,
                        "longitude": 105.7847573,
                        "name": "Destination"
                      }
                    }
                  ],
                  "startTime": "1795769562000",
                  "transfers": 0,
                  "walkSeconds": 4830
                }
              ]
            }
        """.trimIndent()
    }
}
