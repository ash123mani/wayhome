package com.wayhome.domain.repository

import com.wayhome.domain.model.Location

/** Driving route between two or more waypoints, in visit order. */
data class DrivingRoute(
    val distanceMeters: Double,
    val durationSeconds: Double,
    /** Polyline points as [Location(lat, lng)], non-null and valid. */
    val shape: List<Location>
)

/**
 * Road routing. Implementations must never throw — failures are returned as
 * [Result.failure] so callers can report [com.wayhome.domain.routing.RouteMatchStatus.UNAVAILABLE].
 */
interface RouteRepository {
    suspend fun drivingRoute(waypoints: List<Location>): Result<DrivingRoute>
}
