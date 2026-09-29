package com.vie.mit.data.repository.map

import com.vie.mit.data.network.api.map.MapApi
import com.vie.mit.data.network.model.map.Location
import com.vie.mit.data.network.model.map.PublicTransportSuggestion
import com.vie.mit.data.network.util.handleApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepository @Inject constructor(
    private val mapApi: MapApi,
) {
    suspend fun getPublicTransportSuggestions(
        location: Location,
    ): Result<List<PublicTransportSuggestion>> = handleApiCall {
        mapApi.getPublicTransportSuggestions(location)
    }
}
