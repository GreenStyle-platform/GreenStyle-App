package com.vie.mit.data.network.domainmodel.map

import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.model.remote.response.map.HereAutosuggestResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HereAutosuggestApiModelTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun hereAutosuggestResponse_decodesBackendPayloadWithCategoriesObjects() {
        val payload = """
        {
          "items": [
            {
              "title": "Đường Đại Học Hà Nội, Phường Trung Văn, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
              "id": "here:af:street:HpZWQxW83jJOcbqaWh91nC",
              "resultType": "street",
              "href": null,
              "noResultOnFollowUp": null,
              "address": {
                "label": "Đường Đại Học Hà Nội, Phường Trung Văn, Quận Nam Từ Liêm, Hà Nội, Việt Nam",
                "countryCode": null,
                "countryName": null,
                "stateCode": null,
                "state": null,
                "countyCode": null,
                "county": null,
                "city": null,
                "district": null,
                "subdistrict": null,
                "street": null,
                "streets": null,
                "block": null,
                "subblock": null,
                "postalCode": null,
                "houseNumber": null,
                "building": null,
                "unit": null
              },
              "position": {
                "lat": 20.98801,
                "lng": 105.79681
              },
              "distance": 2976,
              "categories": null
            },
            {
              "title": "Đại Học Đại Nam",
              "id": "here:pds:place:704jx7ps-49fa3dc92fe9047d1bee55001ad1e345",
              "resultType": "place",
              "href": null,
              "noResultOnFollowUp": null,
              "address": {
                "label": "Đại Học Đại Nam, Phố Xốm, Phường Phú Lãm, Quận Hà Đông, Hà Nội, Việt Nam"
              },
              "position": {
                "lat": 20.94827,
                "lng": 105.75715
              },
              "distance": 6917,
              "categories": [
                {
                  "id": "800-8200-0173",
                  "name": "Trung cấp/Cao đẳng/Đại học",
                  "primary": true
                }
              ]
            },
            {
              "title": "Học Dễ",
              "id": "here:pds:place:704w7epx-2a9b07fabb04df84762a3eb7450f0069",
              "resultType": "place",
              "address": {
                "label": "Học Dễ, Hẻm 6 Đường Đình Thôn, Phường Mỹ Đình 1, Quận Nam Từ Liêm, Hà Nội, Việt Nam"
              },
              "position": {
                "lat": 21.01812,
                "lng": 105.77624
              },
              "distance": 1177,
              "categories": [
                {
                  "id": "600-6500-0072",
                  "name": "Cửa hàng điện tử gia dụng",
                  "primary": true
                },
                {
                  "id": "600-6500-0073",
                  "name": "Cửa hàng bán lẻ điện thoại di động",
                  "primary": null
                }
              ]
            }
          ],
          "offset": null,
          "nextOffset": null,
          "count": null,
          "limit": null,
          "queryTerms": []
        }
        """.trimIndent()

        val response = json.decodeFromString<HereAutosuggestResponse>(payload)

        assertEquals(3, response.items.size)
        assertNull(response.items[0].categories)

        val placeCategories = response.items[1].categories
        assertEquals(1, placeCategories?.size)
        assertEquals("800-8200-0173", placeCategories?.first()?.id)
        assertEquals("Trung cấp/Cao đẳng/Đại học", placeCategories?.first()?.name)
        assertEquals(true, placeCategories?.first()?.primary)

        val multiCategories = response.items[2].categories
        assertEquals(2, multiCategories?.size)
        assertNull(multiCategories?.get(1)?.primary)

        val domain = response.items[1].toDomain()
        assertEquals("here:pds:place:704jx7ps-49fa3dc92fe9047d1bee55001ad1e345", domain.id)
        assertEquals("Đại Học Đại Nam", domain.title)
        assertEquals(20.94827, domain.latitude!!, 0.00001)
        assertEquals(105.75715, domain.longitude!!, 0.00001)
        assertEquals(6917L, domain.distance)
    }

    @Test
    fun hereAutosuggestResponse_decodesObjectQueryTermsWithoutError() {
        val payload = """
        {
          "items": [],
          "queryTerms": [
            {
              "completion": "học viện",
              "replaces": "học",
              "start": 0,
              "end": 3
            }
          ]
        }
        """.trimIndent()

        val response = json.decodeFromString<HereAutosuggestResponse>(payload)
        assertTrue(response.items.isEmpty())
        assertEquals(1, response.queryTerms.size)
        assertEquals("học viện", response.queryTerms.first().completion)
        assertEquals("học", response.queryTerms.first().replaces)
        assertEquals(0, response.queryTerms.first().start)
        assertEquals(3, response.queryTerms.first().end)
    }
}
