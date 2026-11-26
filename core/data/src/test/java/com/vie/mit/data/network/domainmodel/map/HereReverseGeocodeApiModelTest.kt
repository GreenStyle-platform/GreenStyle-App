package com.vie.mit.data.network.domainmodel.map

import com.vie.mit.data.model.remote.response.map.HereReverseGeocodeResponse
import com.vie.mit.data.network.api.geocode.GeocodeApi
import com.vie.mit.data.repository.geocode.ReverseGeocodeResultSelector
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.Query

class HereReverseGeocodeApiModelTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val samplePayload = """
        {
          "items": [
            {
              "title": "Nội Thất HT",
              "id": "here:pds:place:704w7epw-e7ac3088c312a237aba9a6862c8982de",
              "resultType": "place",
              "houseNumberType": null,
              "address": {
                "label": "Nội Thất HT, Ngách 56/26 Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
                "countryCode": "VNM",
                "countryName": "Việt Nam",
                "city": "Quận Nam Từ Liêm",
                "district": "Phường Phú Đô",
                "street": "Ngách 56/26 Lê Quang Đạo",
                "postalCode": "12013"
              },
              "position": { "lat": 21.0101, "lng": 105.76986 },
              "access": [{ "lat": 21.01014, "lng": 105.76972 }],
              "distance": 59,
              "mapView": null,
              "categories": [
                {
                  "id": "600-6600-0080",
                  "name": "Phụ kiện nội thất và gia đình",
                  "primary": true
                }
              ],
              "foodTypes": null
            },
            {
              "title": "Ngách 56/26 Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
              "id": "here:af:streetsection:YNcn0.9WKvXNQqrwy7ACCA",
              "resultType": "street",
              "address": {
                "label": "Ngách 56/26 Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
                "street": "Ngách 56/26 Lê Quang Đạo"
              },
              "position": { "lat": 21.01014, "lng": 105.76972 },
              "distance": 59,
              "mapView": { "west": 105.76937, "south": 21.01014, "east": 105.76996, "north": 21.0113 },
              "categories": null,
              "foodTypes": null
            },
            {
              "title": "Bệnh Viện Đa Khoa Hồng Ngọc Phúc Trường Minh-Cấp Cứu",
              "id": "here:pds:place:704w7epw-54ba32c62ebb0fcf1555b1fe32978928",
              "resultType": "place",
              "address": {
                "label": "Bệnh Viện Đa Khoa Hồng Ngọc Phúc Trường Minh-Cấp Cứu, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam"
              },
              "distance": 61,
              "categories": [
                {
                  "id": "800-8000-0325",
                  "name": "Cấp cứu",
                  "primary": true
                }
              ]
            },
            {
              "title": "Bệnh Viện Đa Khoa Hồng Ngọc Phúc Trường Minh",
              "id": "here:pds:place:704w7epw-67ac5b5313bb51c6a00a6511e4b565a9",
              "resultType": "place",
              "address": {
                "label": "Bệnh Viện Đa Khoa Hồng Ngọc Phúc Trường Minh, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam"
              },
              "distance": 77,
              "categories": [
                {
                  "id": "800-8000-0159",
                  "name": "Bệnh viện",
                  "primary": true
                }
              ]
            },
            {
              "title": "Duy Linh Quán",
              "id": "here:pds:place:704w7epw-7684582732fc4794a090a341cb4ec96a",
              "resultType": "place",
              "address": {
                "label": "Duy Linh Quán, Đường Châu Văn Liêm, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam"
              },
              "distance": 112,
              "categories": [
                {
                  "id": "100-1000-0000",
                  "name": "Nhà hàng",
                  "primary": true
                }
              ],
              "foodTypes": [{ "id": "206-000", "name": "Việt Nam", "primary": true }]
            },
            {
              "title": "Lẩu Ngon Quán 56 Lê Quang Đạo",
              "id": "here:pds:place:704w7epw-1dddd212bec30394166f855141952555",
              "resultType": "place",
              "address": {
                "label": "Lẩu Ngon Quán 56 Lê Quang Đạo, 56 Đường Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam"
              },
              "distance": 121,
              "categories": [
                {
                  "id": "700-7200-0253",
                  "name": "Dịch vụ ăn uống và thực phẩm",
                  "primary": true
                }
              ]
            },
            {
              "title": "Tigi",
              "id": "here:pds:place:704w7epw-44adce6175db7c79681b5c98cb3fac5b",
              "resultType": "place",
              "address": {
                "label": "Tigi, Ngách 8/11 Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam"
              },
              "distance": 150,
              "categories": [
                {
                  "id": "600-6950-0000",
                  "name": "Cắt tóc & Tạo mẫu",
                  "primary": true
                }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun hereReverseGeocodeResponse_decodesMinimalFieldsIgnoringExtraJsonProperties() {
        val response = json.decodeFromString<HereReverseGeocodeResponse>(samplePayload)

        assertEquals(7, response.items.size)
        val first = response.items.first()
        assertEquals("Nội Thất HT", first.title)
        assertEquals("place", first.resultType)
        assertEquals(
            "Nội Thất HT, Ngách 56/26 Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
            first.address?.label,
        )
        assertEquals(59L, first.distance)
        assertEquals("Phụ kiện nội thất và gia đình", first.categories?.first()?.name)
        assertEquals(true, first.categories?.first()?.primary)
    }

    @Test
    fun geocodeApi_reverseGeocode_usesExpectedGetAnnotationAndQueryParameters() {
        val method = GeocodeApi::class.java.methods.single { it.name == "reverseGeocode" }
        val getAnnotation = method.getAnnotation(GET::class.java)
        assertEquals("here-geocode/reverse-geocode", getAnnotation?.value)

        val queryNames = method.parameterAnnotations
            .flatMap { annotations -> annotations.filterIsInstance<Query>() }
            .map { it.value }
        assertEquals(listOf("latitude", "longitude", "limit"), queryNames)
    }

    @Test
    fun selectBestLabel_returnsClosestItemWhenQueryBlankOrShorterThanThreeChars() {
        val items = json.decodeFromString<HereReverseGeocodeResponse>(samplePayload).items

        assertFalse(ReverseGeocodeResultSelector.hasEffectiveQuery(null))
        assertFalse(ReverseGeocodeResultSelector.hasEffectiveQuery("  ab  "))
        assertTrue(ReverseGeocodeResultSelector.hasEffectiveQuery("  abc "))

        assertEquals(
            "Nội Thất HT, Ngách 56/26 Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
            ReverseGeocodeResultSelector.selectBestLabel(items, queryHint = null),
        )
        assertEquals(
            "Nội Thất HT, Ngách 56/26 Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
            ReverseGeocodeResultSelector.selectBestLabel(items, queryHint = "  be  "),
        )
    }

    @Test
    fun selectBestLabel_matchesVietnameseWithoutDiacriticsAcrossTitleLabelAndCategories() {
        val items = json.decodeFromString<HereReverseGeocodeResponse>(samplePayload).items

        // Matches both title and primary category -> picks main hospital over emergency sub-entry
        assertEquals(
            "Bệnh Viện Đa Khoa Hồng Ngọc Phúc Trường Minh, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
            ReverseGeocodeResultSelector.selectBestLabel(items, queryHint = "benh vien"),
        )

        // Matches title -> picks closest match (61m vs 77m)
        assertEquals(
            "Bệnh Viện Đa Khoa Hồng Ngọc Phúc Trường Minh-Cấp Cứu, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
            ReverseGeocodeResultSelector.selectBestLabel(items, queryHint = "hong ngoc"),
        )

        // Matches primary category name when label does not contain the category keyword
        assertEquals(
            "Duy Linh Quán, Đường Châu Văn Liêm, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
            ReverseGeocodeResultSelector.selectBestLabel(items, queryHint = "nha hang"),
        )
        assertEquals(
            "Tigi, Ngách 8/11 Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
            ReverseGeocodeResultSelector.selectBestLabel(items, queryHint = "cắt tóc"),
        )

        // Handles Vietnamese 'đ' normalization and prefers title match over label-only match
        assertEquals(
            "Ngách 56/26 Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
            ReverseGeocodeResultSelector.selectBestLabel(items, queryHint = "le quang dao"),
        )

        // Multi-word fallback when words are non-adjacent
        assertEquals(
            "Lẩu Ngon Quán 56 Lê Quang Đạo, 56 Đường Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
            ReverseGeocodeResultSelector.selectBestLabel(items, queryHint = "lau le quang dao"),
        )

        // Unmatched query gracefully falls back to closest result
        assertEquals(
            "Nội Thất HT, Ngách 56/26 Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
            ReverseGeocodeResultSelector.selectBestLabel(items, queryHint = "khong ton tai"),
        )
    }

    @Test
    fun selectBestLabel_returnsNullForEmptyList() {
        assertNull(ReverseGeocodeResultSelector.selectBestLabel(emptyList(), queryHint = "benh vien"))
    }
}
