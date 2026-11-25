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
    @SerialName("countryCode") val countryCode: String? = null,
    @SerialName("countryName") val countryName: String? = null,
    @SerialName("stateCode") val stateCode: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("countyCode") val countyCode: String? = null,
    @SerialName("county") val county: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("district") val district: String? = null,
    @SerialName("subdistrict") val subdistrict: String? = null,
    @SerialName("street") val street: String? = null,
    @SerialName("streets") val streets: List<String>? = null,
    @SerialName("block") val block: String? = null,
    @SerialName("subblock") val subblock: String? = null,
    @SerialName("postalCode") val postalCode: String? = null,
    @SerialName("houseNumber") val houseNumber: String? = null,
    @SerialName("building") val building: String? = null,
    @SerialName("unit") val unit: String? = null,
)

@Serializable
data class HerePositionResponse(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
)
