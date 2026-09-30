package com.wayhome.data.routing

import kotlinx.serialization.Serializable

@Serializable
data class OsrmRouteResponse(
    val code: String = "",
    val routes: List<OsrmRoute> = emptyList(),
    val message: String? = null
)

@Serializable
data class OsrmRoute(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val geometry: GeoJsonLineString = GeoJsonLineString()
)

@Serializable
data class GeoJsonLineString(
    val type: String = "",
    /** OSRM encodes positions as [longitude, latitude]. */
    val coordinates: List<List<Double>> = emptyList()
)
