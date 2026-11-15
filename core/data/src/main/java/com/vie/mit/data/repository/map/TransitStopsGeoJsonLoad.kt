package com.vie.mit.data.repository.map

data class TransitStopsGeoJsonLoad(
    val geoJsonByType: Map<String, String>,
    val failedTypeIds: Set<String>,
)
