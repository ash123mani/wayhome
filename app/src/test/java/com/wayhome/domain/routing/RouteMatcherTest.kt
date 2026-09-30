package com.wayhome.domain.routing

import com.wayhome.domain.model.Location
import com.wayhome.domain.repository.DrivingRoute
import com.wayhome.domain.repository.RouteRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMatcherTest {
    private fun loc(lat: Double?, lng: Double?) = Location(lat, lng)

    private class FakeRouteRepository(
        var handler: (List<Location>) -> Result<DrivingRoute> = {
            Result.failure(IllegalStateException("no stub"))
        }
    ) : RouteRepository {
        var calls = 0
        override suspend fun drivingRoute(waypoints: List<Location>): Result<DrivingRoute> {
            calls++
            return handler(waypoints)
        }
    }

    /** Straightforward stand-in for OSRM: road distance = haversine legs. */
    private fun haversineRoute(waypoints: List<Location>): DrivingRoute {
        val distance = waypoints.windowed(2).sumOf { (a, b) ->
            GeoUtils.distanceBetween(a, b).let { if (it < 0.0) 0.0 else it }
        }
        return DrivingRoute(distance, distance / 10.0, waypoints)
    }

    private val origin = loc(0.0, 0.0)
    private val destinationA = loc(0.0, 0.1) // ~11.1 km east of origin

    private fun matcher(
        repo: FakeRouteRepository,
        config: RouteMatchConfig = RouteMatchConfig()
    ) = OsrmRouteMatcher(repo, config)

    @Test
    fun `B on the route matches with no detour`() = runBlocking {
        val repo = FakeRouteRepository { Result.success(haversineRoute(it)) }
        val result = matcher(repo).match(origin, destinationA, loc(0.0, 0.05))

        assertTrue(result.matched)
        assertEquals(RouteMatchStatus.MATCH, result.status)
        assertEquals(0.0, result.distanceFromRouteMeters, 1.0)
        assertEquals(0.0, result.detourMeters, 1.0)
        assertEquals(2, repo.calls) // direct + via
    }

    @Test
    fun `B about 1km from the route matches`() = runBlocking {
        val repo = FakeRouteRepository { Result.success(haversineRoute(it)) }
        val result = matcher(repo).match(origin, destinationA, loc(0.008, 0.05))

        assertTrue(result.matched)
        assertTrue(result.distanceFromRouteMeters in 800.0..1_000.0)
        assertTrue(result.detourMeters in 0.0..500.0)
        assertTrue(result.partnerPositionOnRouteMeters > 0.0)
    }

    @Test
    fun `B just inside 2km matches`() = runBlocking {
        val repo = FakeRouteRepository { Result.success(haversineRoute(it)) }
        val result = matcher(repo).match(origin, destinationA, loc(0.0179, 0.05))

        assertTrue(result.matched)
        assertTrue(result.distanceFromRouteMeters <= 2_000.0)
    }

    @Test
    fun `B beyond 2km is rejected without a via lookup`() = runBlocking {
        val repo = FakeRouteRepository { Result.success(haversineRoute(it)) }
        val result = matcher(repo).match(origin, destinationA, loc(0.019, 0.05))

        assertFalse(result.matched)
        assertEquals(RouteMatchStatus.NOT_A_MATCH, result.status)
        assertTrue(result.distanceFromRouteMeters > 2_000.0)
        assertEquals(UNKNOWN_METERS, result.viaPartnerDistanceMeters, 0.0)
        assertEquals(1, repo.calls) // lateral short-circuit: direct only
    }

    @Test
    fun `B beyond the destination is rejected by the default direction rule`() = runBlocking {
        val repo = FakeRouteRepository { Result.success(haversineRoute(it)) }
        val nearA = loc(0.0, 0.05)
        val beyondA = loc(0.0, 0.09) // ~4.4 km past A along the road

        val result = matcher(repo).match(origin, nearA, beyondA)

        assertFalse(result.matched)
        assertEquals(RouteMatchStatus.NOT_A_MATCH, result.status)
        assertEquals(1, repo.calls) // rejected before the via lookup
    }

    @Test
    fun `B slightly past the destination still matches within tolerance`() = runBlocking {
        val repo = FakeRouteRepository { Result.success(haversineRoute(it)) }
        val nearA = loc(0.0, 0.05)

        val result = matcher(repo).match(origin, nearA, loc(0.0, 0.054))

        assertTrue(result.matched)
        assertEquals(RouteMatchStatus.MATCH, result.status)
    }

    @Test
    fun `large detour is rejected even when beyond-destination is allowed`() = runBlocking {
        // Road networks can force long detours for laterally-close points
        // (one-ways, U-turns); the fake backend returns a canned 27 km via
        // route to simulate exactly that.
        val repo = FakeRouteRepository { waypoints ->
            if (waypoints.size > 2) Result.success(DrivingRoute(27_000.0, 0.0, waypoints))
            else Result.success(haversineRoute(waypoints))
        }
        val config = RouteMatchConfig(allowBeyondDestination = true)

        val result = matcher(repo, config).match(origin, destinationA, loc(0.008, 0.05))

        assertFalse(result.matched)
        assertEquals(RouteMatchStatus.NOT_A_MATCH, result.status)
        assertTrue(result.distanceFromRouteMeters <= 2_000.0)
        assertTrue(result.detourMeters > 5_000.0)
        assertEquals(2, repo.calls)
    }

    @Test
    fun `OSRM failure on the direct route is unavailable, not a mismatch`() = runBlocking {
        val repo = FakeRouteRepository { Result.failure(IllegalStateException("OSRM down")) }
        val result = matcher(repo).match(origin, destinationA, loc(0.0, 0.05))

        assertFalse(result.matched)
        assertEquals(RouteMatchStatus.UNAVAILABLE, result.status)
        assertEquals(UNKNOWN_METERS, result.directDistanceMeters, 0.0)
    }

    @Test
    fun `OSRM failure on the via route is unavailable`() = runBlocking {
        val repo = FakeRouteRepository { waypoints ->
            if (waypoints.size > 2) Result.failure(IllegalStateException("OSRM down"))
            else Result.success(haversineRoute(waypoints))
        }
        val result = matcher(repo).match(origin, destinationA, loc(0.0, 0.05))

        assertFalse(result.matched)
        assertEquals(RouteMatchStatus.UNAVAILABLE, result.status)
        // Unavailable blanks every metric so callers can't mistake it for a verdict.
        assertEquals(UNKNOWN_METERS, result.directDistanceMeters, 0.0)
        assertEquals(UNKNOWN_METERS, result.detourMeters, 0.0)
    }

    @Test
    fun `invalid coordinates are unavailable without backend calls`() = runBlocking {
        val repo = FakeRouteRepository { Result.success(haversineRoute(it)) }
        val result = matcher(repo).match(loc(null, 0.0), destinationA, loc(0.0, 0.05))

        assertEquals(RouteMatchStatus.UNAVAILABLE, result.status)
        assertEquals(0, repo.calls)
    }

    // Spec examples, straight against the rule evaluation.
    @Test
    fun `spec example - 1km off route with 2km detour matches`() {
        assertEquals(
            RouteMatchStatus.MATCH,
            decideRouteMatch(RouteMatchConfig(), 1_000.0, 2_000.0, 0.0)
        )
    }

    @Test
    fun `spec example - 3km off route does not match`() {
        assertEquals(
            RouteMatchStatus.NOT_A_MATCH,
            decideRouteMatch(RouteMatchConfig(), 3_000.0, 2_000.0, 0.0)
        )
    }

    @Test
    fun `spec example - 7km detour does not match`() {
        assertEquals(
            RouteMatchStatus.NOT_A_MATCH,
            decideRouteMatch(RouteMatchConfig(), 1_000.0, 7_000.0, 0.0)
        )
    }

    @Test
    fun `result keeps matched and status consistent`() {
        assertEquals(RouteMatchStatus.MATCH, RouteMatchResult(true, 1.0, 2.0, 3.0, 4.0, 5.0).status)
        assertThrows(IllegalArgumentException::class.java) {
            RouteMatchResult(true, 1.0, 2.0, 3.0, 4.0, 5.0, RouteMatchStatus.NOT_A_MATCH)
        }
    }
}
