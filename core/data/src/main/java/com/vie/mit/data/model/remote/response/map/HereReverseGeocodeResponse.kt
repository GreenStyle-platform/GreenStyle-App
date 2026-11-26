package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HereReverseGeocodeResponse(
    @SerialName("items") val items: List<HereReverseGeocodeItemResponse> = emptyList(),
)

@Serializable
data class HereReverseGeocodeItemResponse(
    @SerialName("title") val title: String? = null,
    @SerialName("resultType") val resultType: String? = null,
    @SerialName("address") val address: HereAddressResponse? = null,
    @SerialName("distance") val distance: Long? = null,
    @SerialName("categories") val categories: List<HereCategoryResponse>? = null,
)
