package com.vie.mit.data.network.model.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicTransportSuggestion(
    @SerialName("name") val name: String,
    @SerialName("type") val type: PublicTransportType,
    @SerialName("distance") val distance: Long,
    @SerialName("availableIn") val availableIn: Int,
)
