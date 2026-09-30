package com.wayhome.domain.routing

import com.wayhome.domain.model.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Nearest-point analysis of a target against a route polyline. */
data class RouteProjection(
    val distanceFromRouteMeters: Double,
    val positionOnRouteMeters: Double,
    val segmentIndex: Int,
    val overshootBeyondEndMeters: Double,
    val routeLengthMeters: Double
)

/**
 * Reusable geographic math. All distances are meters, all results are finite —
 * invalid input yields [UNKNOWN_METERS], never NaN/Infinity.
 */
object GeoUtils {
    const val EARTH_RADIUS_METERS = 6_371_000.0

    fun isValid(location: Location?): Boolean {
        if (location == null) return false
        val lat = location.lat ?: return false
        val lng = location.lng ?: return false
        if (!lat.isFinite() || !lng.isFinite()) return false
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }

    /** Haversine. 0 for identical points, [UNKNOWN_METERS] for invalid input. */
    fun distanceBetween(a: Location?, b: Location?): Double {
        if (!isValid(a) || !isValid(b)) return UNKNOWN_METERS
        val lat1 = Math.toRadians(a!!.lat!!)
        val lat2 = Math.toRadians(b!!.lat!!)
        val dLat = Math.toRadians(b.lat!! - a.lat!!)
        val dLon = Math.toRadians(b.lng!! - a.lng!!)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = h.coerceIn(0.0, 1.0)
        val d = 2 * EARTH_RADIUS_METERS * atan2(sqrt(c), sqrt(1 - c))
        return if (d.isFinite() && d >= 0.0) d else UNKNOWN_METERS
    }

    /**
     * Shortest distance from [point] to segment [start]→[end].
     * Zero-length segments degrade to [distanceBetween]; invalid input
     * yields [UNKNOWN_METERS].
     */
    fun distanceToSegment(point: Location?, start: Location?, end: Location?): Double {
        if (!isValid(point) || !isValid(start) || !isValid(end)) return UNKNOWN_METERS
        val (t, _) = projectMeters(start!!, end!!, point!!)
        val closest = interpolate(start, end, t) ?: return distanceBetween(point, start)
        val d = distanceBetween(point, closest)
        return if (d.isFinite() && d >= 0.0) d else UNKNOWN_METERS
    }

    /**
     * Nearest point of [target] on polyline [shape]: lateral distance, index
     * of the nearest segment, along-route position of the projection, route
     * length, and how far [target] lies past the final endpoint along the
     * last segment's heading (0 when alongside or before it).
     * Null when the shape is unusable (< 2 points or any invalid point).
     */
    fun projectionOnRoute(shape: List<Location>, target: Location): RouteProjection? {
        if (shape.size < 2 || !isValid(target)) return null
        if (shape.any { !isValid(it) }) return null

        var best = Double.MAX_VALUE
        var bestPos = 0.0
        var bestIdx = 0
        var cumulative = 0.0
        shape.windowed(2).forEachIndexed { index, (s, e) ->
            val segLen = distanceBetween(s, e)
            if (segLen < 0) return null
            val (t, _) = projectMeters(s, e, target)
            val closest = interpolate(s, e, t) ?: return null
            val d = distanceBetween(target, closest)
            if (d < 0) return null
            if (d < best) {
                best = d
                bestPos = cumulative + t * segLen
                bestIdx = index
            }
            cumulative += segLen
        }
        val lastStart = shape[shape.size - 2]
        val lastEnd = shape[shape.size - 1]
        val (_, alongEnd) = projectMeters(lastStart, lastEnd, target)
        val lastLen = distanceBetween(lastStart, lastEnd)
        if (lastLen < 0) return null
        val overshoot = max(0.0, alongEnd - lastLen)
        if (!overshoot.isFinite()) return null
        return RouteProjection(best, bestPos, bestIdx, overshoot, cumulative)
    }

    /**
     * Planar projection in meters around the segment's mean latitude
     * (accurate to centimetres at the distances we care about).
     * Returns clamped t in 0..1 plus unclamped along-track meters from [s].
     */
    private fun projectMeters(s: Location, e: Location, p: Location): Pair<Double, Double> {
        val latRef = Math.toRadians((s.lat!! + e.lat!! + p.lat!!) / 3.0)
        val k = cos(latRef) * EARTH_RADIUS_METERS
        fun x(lng: Double) = Math.toRadians(lng) * k
        fun y(lat: Double) = Math.toRadians(lat) * EARTH_RADIUS_METERS
        val dx = x(e.lng!!) - x(s.lng!!)
        val dy = y(e.lat!!) - y(s.lat!!)
        val rx = x(p.lng!!) - x(s.lng!!)
        val ry = y(p.lat!!) - y(s.lat!!)
        val lenSq = dx * dx + dy * dy
        if (!lenSq.isFinite() || lenSq <= 0.0) return 0.0 to 0.0
        val dot = rx * dx + ry * dy
        if (!dot.isFinite()) return 0.0 to 0.0
        val len = sqrt(lenSq)
        val along = dot / len
        val t = (dot / lenSq).coerceIn(0.0, 1.0)
        if (!along.isFinite() || !t.isFinite()) return 0.0 to 0.0
        return t to along
    }

    /** Linear interpolation between two valid points (t in 0..1). */
    private fun interpolate(s: Location, e: Location, t: Double): Location? {
        val lat = s.lat!! + (e.lat!! - s.lat!!) * t
        val lng = s.lng!! + (e.lng!! - s.lng!!) * t
        val out = Location(lat, lng)
        return if (isValid(out)) out else null
    }
}
