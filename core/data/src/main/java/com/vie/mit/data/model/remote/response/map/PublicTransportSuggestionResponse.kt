package com.vie.mit.data.model.remote.response.map

import com.vie.mit.data.model.common.map.TransitMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicTransportSuggestionResponse(
    @SerialName("name") val name: String,
    @SerialName("type") val type: TransitMode,
    @SerialName("distance") val distance: Long,
    @SerialName("availableIn") val availableIn: Int,
)
