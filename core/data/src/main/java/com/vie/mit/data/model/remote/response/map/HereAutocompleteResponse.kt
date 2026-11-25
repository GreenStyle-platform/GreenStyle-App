package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HereAutocompleteResponse(
    @SerialName("items") val items: List<HereAutocompleteItemResponse> = emptyList(),
)

@Serializable
data class HereAutocompleteItemResponse(
    @SerialName("title") val title: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("resultType") val resultType: String? = null,
    @SerialName("localityType") val localityType: String? = null,
    @SerialName("address") val address: HereAddressResponse? = null,
    @SerialName("distance") val distance: Long? = null,
    @SerialName("highlights") val highlights: HereHighlightsResponse? = null,
)

@Serializable
data class HereHighlightsResponse(
    @SerialName("title") val title: List<HereHighlightRangeResponse>? = null,
    @SerialName("address") val address: HereAddressHighlightsResponse? = null,
)

@Serializable
data class HereHighlightRangeResponse(
    @SerialName("start") val start: Int,
    @SerialName("end") val end: Int,
)

@Serializable
data class HereAddressHighlightsResponse(
    @SerialName("label") val label: List<HereHighlightRangeResponse>? = null,
    @SerialName("city") val city: List<HereHighlightRangeResponse>? = null,
)
