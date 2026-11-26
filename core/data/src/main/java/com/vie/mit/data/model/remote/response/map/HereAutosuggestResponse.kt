package com.vie.mit.data.model.remote.response.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HereAutosuggestResponse(
    @SerialName("items") val items: List<HereAutosuggestItemResponse> = emptyList(),
    @SerialName("offset") val offset: Int? = null,
    @SerialName("nextOffset") val nextOffset: Int? = null,
    @SerialName("count") val count: Int? = null,
    @SerialName("limit") val limit: Int? = null,
    @SerialName("queryTerms") val queryTerms: List<HereQueryTermResponse> = emptyList(),
)

@Serializable
data class HereQueryTermResponse(
    @SerialName("completion") val completion: String? = null,
    @SerialName("replaces") val replaces: String? = null,
    @SerialName("start") val start: Int? = null,
    @SerialName("end") val end: Int? = null,
)

@Serializable
data class HereAutosuggestItemResponse(
    @SerialName("title") val title: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("resultType") val resultType: String? = null,
    @SerialName("href") val href: String? = null,
    @SerialName("noResultOnFollowUp") val noResultOnFollowUp: Boolean? = null,
    @SerialName("address") val address: HereAddressResponse? = null,
    @SerialName("position") val position: HerePositionResponse? = null,
    @SerialName("distance") val distance: Long? = null,
    @SerialName("categories") val categories: List<HereCategoryResponse>? = null,
)

@Serializable
data class HereCategoryResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("primary") val primary: Boolean? = null,
)

@Serializable
data class HereAddressResponse(
    @SerialName("label") val label: String? = null,
)

@Serializable
data class HerePositionResponse(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
)
