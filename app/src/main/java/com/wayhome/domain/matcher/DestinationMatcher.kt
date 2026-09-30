package com.wayhome.domain.matcher

import com.wayhome.domain.model.Destination
import com.wayhome.domain.model.Location
import com.wayhome.domain.model.MatchLevel
import com.wayhome.domain.model.MatchResult
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

interface DestinationMatcher {
    fun calculateMatch(
        source: Location?,
        destinationA: Destination,
        destinationB: Destination
    ): MatchResult
}

/**
 * MVP matcher: haversine on seeded lat/lng when available, else
 * city + normalized area string comparison. thresholds:
 * SAME_AREA < 2km (or equal area string), NEARBY < 6km, SAME_CITY else.
 * Replace with route-based matching later without touching UI/transport.
 */
class HaversineDestinationMatcher @Inject constructor() : DestinationMatcher {
    override fun calculateMatch(
        source: Location?,
        destinationA: Destination,
        destinationB: Destination
    ): MatchResult {
        val a = destinationA.location
        val b = destinationB.location
        if (a.lat != null && a.lng != null && b.lat != null && b.lng != null) {
            val km = haversineKm(a.lat, a.lng, b.lat, b.lng)
            val level = when {
                km < 2.0 -> MatchLevel.SAME_AREA
                km < 6.0 -> MatchLevel.NEARBY_AREA
                destinationA.city.equals(destinationB.city, ignoreCase = true) -> MatchLevel.SAME_CITY
                else -> MatchLevel.DIFFERENT
            }
            return MatchResult(level, km)
        }
        // Fallback: string comparison (custom city/area entries).
        val sameCity = destinationA.city.equals(destinationB.city, ignoreCase = true)
        val sameArea = destinationA.area.trim().equals(destinationB.area.trim(), ignoreCase = true)
        val level = when {
            sameCity && sameArea -> MatchLevel.SAME_AREA
            sameCity -> MatchLevel.SAME_CITY
            else -> MatchLevel.DIFFERENT
        }
        return MatchResult(level, null)
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(h), sqrt(1 - h))
    }
}
