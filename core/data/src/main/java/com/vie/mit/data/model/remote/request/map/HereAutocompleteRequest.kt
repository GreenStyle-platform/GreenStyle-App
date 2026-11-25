package com.vie.mit.data.model.remote.request.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HereAutocompleteRequest(
    @SerialName("query") val query: String,
    @SerialName("latitudeIn") val latitudeIn: Double,
    @SerialName("longitudeIn") val longitudeIn: Double,
)
