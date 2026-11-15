package com.vie.mit.data.model.domain.map

data class TransitStopsGeoJsonLoad(
    val geoJsonByType: Map<String, String>,
    val failedTypeIds: Set<String>,
)
