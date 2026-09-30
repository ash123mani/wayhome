package com.wayhome.data.routing

import com.wayhome.domain.model.Location
import com.wayhome.domain.repository.DrivingRoute
import com.wayhome.domain.repository.RouteRepository
import com.wayhome.domain.routing.GeoUtils
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OSRM-backed [RouteRepository]. Never throws — every failure (invalid
 * input, HTTP error, bad payload, empty route) is a [Result.failure].
 *
 * Successful `origin → destination` lookups are cached in memory so ranking
 * N candidates reuses one Airport→A route instead of fetching it N times.
 */
@Singleton
class OsrmRouteRepository @Inject constructor(
    private val api: OsrmApi
) : RouteRepository {
    private val lock = Any()
    private val cache = object : LinkedHashMap<String, DrivingRoute>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DrivingRoute>): Boolean =
            size > MAX_CACHED_ROUTES
    }

    override suspend fun drivingRoute(waypoints: List<Location>): Result<DrivingRoute> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(waypoints.size >= 2) { "Need at least 2 waypoints" }
                require(waypoints.all { GeoUtils.isValid(it) }) { "Invalid waypoint coordinates" }
                val key = waypoints.joinToString("|") { formatPoint(it) }
                synchronized(lock) { cache[key] }?.let { return@runCatching it }

                // OSRM takes longitude,latitude — never latitude,longitude.
                val coordinates = waypoints.joinToString(";") { wp ->
                    String.format(Locale.US, "%.6f,%.6f", wp.lng!!, wp.lat!!)
                }
                val response = api.getRoute(coordinates)
                require(response.code == "Ok") {
                    "OSRM error: ${response.message ?: response.code}"
                }
                val route = response.routes.firstOrNull()
                    ?: throw IllegalStateException("OSRM returned no routes")
                require(route.distance.isFinite() && route.distance >= 0.0) {
                    "OSRM returned an invalid distance"
                }
                val shape = route.geometry.coordinates.map { pair ->
                    require(pair.size >= 2) { "OSRM returned a malformed position" }
                    val lng = pair[0]
                    val lat = pair[1]
                    val point = Location(lat, lng)
                    require(GeoUtils.isValid(point)) { "OSRM returned out-of-range coordinates" }
                    point
                }
                require(shape.size >= 2) { "OSRM returned an empty geometry" }
                val driving = DrivingRoute(route.distance, route.duration, shape)
                synchronized(lock) { cache[key] = driving }
                driving
            }
        }

    private fun formatPoint(location: Location): String =
        String.format(Locale.US, "%.6f,%.6f", location.lat, location.lng)

    companion object {
        const val MAX_CACHED_ROUTES = 64
    }
}
