package com.vie.mit.data.repository.routeplanner

import com.vie.mit.data.model.domain.map.Location
import com.vie.mit.data.model.domain.map.PublicTransportSuggestion
import com.vie.mit.data.model.domain.map.RouteOption
import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.mapper.toLocationRequest
import com.vie.mit.data.model.remote.request.map.TransitRouteSearchRequest
import com.vie.mit.data.network.api.routeplanner.RoutePlannerApi
import com.vie.mit.data.network.util.handleApiCall
import com.vie.mit.data.repository.routeplanner.RoutePlannerRepository.Companion.DEFAULT_DEPART_NOW_BUFFER_MS
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutePlannerRepository @Inject constructor(
    private val routePlannerApi: RoutePlannerApi,
) {
    companion object {
        const val DEFAULT_MAX_ITINERARIES = 5

        /**
         * Forward buffer applied when searching "depart now" to absorb network latency
         * and minor device/emulator clock drift relative to backend UTC time.
         */
        const val DEFAULT_DEPART_NOW_BUFFER_MS = 60_000L

        /**
         * Forward buffer applied on a single retry synced to the server timestamp.
         */
        const val RETRY_DEPART_NOW_BUFFER_MS = 30_000L

        private val ISO_UTC_REGEX =
            Regex("""^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d+))?(?:Z|\+00:00)$""")

        fun formatUtcIsoTimestamp(epochMillis: Long): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return formatter.format(Date(epochMillis))
        }

        internal fun parseUtcIsoTimestampMillis(isoString: String?): Long? {
            val raw = isoString?.trim().orEmpty()
            if (raw.isEmpty()) return null
            val match = ISO_UTC_REGEX.matchEntire(raw) ?: return null
            val baseDateTime = match.groupValues[1]
            val fracDigits = match.groupValues[2]
            val millisStr = fracDigits.padEnd(3, '0').take(3)
            val normalized = "$baseDateTime.${millisStr}Z"
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
            return runCatching { formatter.parse(normalized)?.time }.getOrNull()
        }
    }

    suspend fun getPublicTransportSuggestions(
        location: Location,
    ): Result<List<PublicTransportSuggestion>> = handleApiCall {
        routePlannerApi.getPublicTransportSuggestions(location.toLocationRequest())
            .map { it.toDomain() }
    }

    /**
     * Searches multimodal transit route options between Start and End coordinates
     * using the OpenTripPlanner 2 backend API (`POST transit-routes/search`).
     *
     * When [departureTimeMillis] is `null` ("depart now"), a small forward buffer
     * ([DEFAULT_DEPART_NOW_BUFFER_MS]) is applied to avoid rejecting requests due to network
     * latency or device clock lag. If the server still rejects the timestamp with HTTP 400/422
     * because the device clock is further behind server UTC time, the call syncs to the server
     * timestamp and retries once automatically.
     */
    suspend fun getRouteOptions(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        departureTimeMillis: Long? = null,
        maxItineraries: Int = DEFAULT_MAX_ITINERARIES,
    ): Result<List<RouteOption>> = handleApiCall {
        val isDepartNow = departureTimeMillis == null
        val requestedDepartureMillis = departureTimeMillis
            ?: (System.currentTimeMillis() + DEFAULT_DEPART_NOW_BUFFER_MS)

        val request = TransitRouteSearchRequest(
            originLatitude = startLat,
            originLongitude = startLng,
            destinationLatitude = endLat,
            destinationLongitude = endLng,
            departureTime = formatUtcIsoTimestamp(requestedDepartureMillis),
            maxItineraries = maxItineraries,
        )

        try {
            routePlannerApi.searchTransitRoutes(request).toDomain()
        } catch (e: HttpException) {
            if (!isDepartNow || (e.code() != 400 && e.code() != 422)) throw e

            val serverTimeMillis = extractServerTimestampMillis(e) ?: throw e
            if (serverTimeMillis < requestedDepartureMillis) throw e

            val syncedDepartureMillis = serverTimeMillis + RETRY_DEPART_NOW_BUFFER_MS
            Timber.w(
                "Device clock lagged behind server UTC by %d ms; retrying transit search with synced departureTime=%s",
                serverTimeMillis - requestedDepartureMillis,
                formatUtcIsoTimestamp(syncedDepartureMillis),
            )
            val retryRequest = request.copy(
                departureTime = formatUtcIsoTimestamp(syncedDepartureMillis),
            )
            routePlannerApi.searchTransitRoutes(retryRequest).toDomain()
        }
    }

    private fun extractServerTimestampMillis(exception: HttpException): Long? {
        val response = exception.response() ?: return null

        // Non-destructively peek errorBody so downstream error handlers can still read it if needed
        val peekedBody = runCatching {
            val source = response.errorBody()?.source() ?: return@runCatching null
            source.request(Long.MAX_VALUE)
            source.buffer.clone().readUtf8()
        }.getOrNull()

        val bodyTimestamp = peekedBody?.let { rawBody ->
            runCatching {
                val jsonObject = Json.parseToJsonElement(rawBody).jsonObject
                parseUtcIsoTimestampMillis(jsonObject["timestamp"]?.jsonPrimitive?.contentOrNull)
            }.getOrNull()
        }
        if (bodyTimestamp != null) return bodyTimestamp

        return runCatching { response.headers().getDate("Date")?.time }.getOrNull()
    }
}
