package com.vie.mit.data.repository.geocode

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.vie.mit.data.model.domain.map.AutosuggestPlace
import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.remote.request.map.HereAutocompleteRequest
import com.vie.mit.data.model.remote.request.map.HereAutosuggestRequest
import com.vie.mit.data.network.api.geocode.GeocodeApi
import com.vie.mit.data.network.util.handleApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class GeocodeRepository(
    private val geocodeApi: GeocodeApi,
) {
    private var appContext: Context? = null

    @Inject
    constructor(
        geocodeApi: GeocodeApi,
        @ApplicationContext context: Context,
    ) : this(geocodeApi) {
        this.appContext = context.applicationContext
    }

    companion object {
        private const val REVERSE_GEOCODE_LIMIT_DEFAULT = 5
        private const val REVERSE_GEOCODE_LIMIT_WITH_QUERY = 20
    }

    /**
     * Searches places and locations using HERE Autosuggest API (returns coordinates).
     */
    suspend fun getAutosuggest(
        query: String,
        latitude: Double,
        longitude: Double,
    ): Result<List<AutosuggestPlace>> = handleApiCall {
        geocodeApi.getAutosuggest(
            HereAutosuggestRequest(
                query = query,
                latitude = latitude,
                longitude = longitude,
            ),
        ).items.map { it.toDomain() }
    }

    /**
     * Suggests keywords and text completions using HERE Autocomplete API.
     */
    suspend fun getAutocomplete(
        query: String,
        latitude: Double,
        longitude: Double,
    ): Result<List<AutosuggestPlace>> = handleApiCall {
        geocodeApi.getAutocomplete(
            HereAutocompleteRequest(
                query = query,
                latitudeIn = latitude,
                longitudeIn = longitude,
            ),
        ).items.map { it.toDomain() }
    }

    /**
     * Reverse geocodes map coordinates into a human-readable address label using the backend
     * HERE reverse-geocode endpoint, falling back to Android [Geocoder] and a coordinate label
     * when unavailable or offline.
     *
     * @param latitude Target latitude.
     * @param longitude Target longitude.
     * @param queryHint Optional text currently typed by the user in the search field to select
     *   the best matching candidate when multiple results are returned.
     * @param limit Optional maximum number of reverse-geocode candidates to request.
     */
    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        queryHint: String? = null,
        limit: Int? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        val fallback = "Vị trí đã chọn (${
            String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
        })"
        val effectiveLimit =
            limit ?: if (ReverseGeocodeResultSelector.hasEffectiveQuery(queryHint)) {
                REVERSE_GEOCODE_LIMIT_WITH_QUERY
            } else {
                REVERSE_GEOCODE_LIMIT_DEFAULT
            }

        try {
            val response = geocodeApi.reverseGeocode(
                latitude = latitude,
                longitude = longitude,
                limit = effectiveLimit,
            )
            val bestLabel = ReverseGeocodeResultSelector.selectBestLabel(
                items = response.items,
                queryHint = queryHint,
            )
            if (!bestLabel.isNullOrBlank()) {
                return@withContext Result.success(bestLabel)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(
                e,
                "HERE reverseGeocode API failed for ($latitude, $longitude), falling back to Android Geocoder",
            )
        }
        val context = appContext
        if (context == null || !Geocoder.isPresent()) {
            return@withContext Result.success(fallback)
        }

        try {
            val geocoder = Geocoder(context, Locale.forLanguageTag("vi-VN"))
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<Address?> { continuation ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) {
                                    continuation.resume(addresses.firstOrNull())
                                }
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        },
                    )
                }
            } else {
                @Suppress("DEPRECATION") geocoder.getFromLocation(latitude, longitude, 1)
                    ?.firstOrNull()
            }
            val label = address?.toDisplayLabel()
            Result.success(if (!label.isNullOrBlank()) label else fallback)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(
                e, "Reverse geocode failed for ($latitude, $longitude), using coordinate fallback"
            )
            Result.success(fallback)
        }
    }

    private fun Address.toDisplayLabel(): String? {
        val line = getAddressLine(0)?.trim()
        if (!line.isNullOrBlank()) return line
        val parts = listOfNotNull(
            featureName?.takeIf { it.isNotBlank() },
            thoroughfare?.takeIf { it.isNotBlank() },
            subLocality?.takeIf { it.isNotBlank() },
            locality?.takeIf { it.isNotBlank() },
            adminArea?.takeIf { it.isNotBlank() },
        ).distinct()
        return parts.joinToString(", ").takeIf { it.isNotBlank() }
    }
}
