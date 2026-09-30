package com.wayhome.domain.usecase

import com.wayhome.domain.model.Destination
import com.wayhome.domain.model.Location
import com.wayhome.domain.model.MatchLevel
import com.wayhome.domain.model.Peer
import com.wayhome.domain.routing.RouteMatchConfig
import com.wayhome.domain.routing.RouteMatchResult
import com.wayhome.domain.routing.RouteMatcher
import com.wayhome.domain.routing.UNKNOWN_METERS
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FindRouteMatchesUseCaseTest {
    private val me = Destination("Bengaluru", "Whitefield", Location(12.9698, 77.7499))
    private val origin = Location(12.9941, 77.6163)
    private val itpl = Destination("Bengaluru", "ITPL", Location(12.9828, 77.7603))
    private val midtown = Destination("Bengaluru", "Midtown", Location(12.93, 77.68)) // ~8.7 km away
    private val electronicCity =
        Destination("Bengaluru", "Electronic City", Location(12.8452, 77.6602)) // ~17 km away
    private val dwarka = Destination("Delhi", "Dwarka", Location(28.5921, 77.0460))

    private fun peer(id: String, destination: Destination) =
        Peer(endpointId = id, tempId = "Traveller $id", destination = destination)

    private fun candidate(id: String, destination: Destination, level: MatchLevel) =
        RouteCandidate(peer(id, destination), level, null)

    private fun noMatch() = RouteMatchResult(
        matched = false,
        distanceFromRouteMeters = 5_000.0,
        directDistanceMeters = 20_000.0,
        viaPartnerDistanceMeters = UNKNOWN_METERS,
        detourMeters = UNKNOWN_METERS,
        partnerPositionOnRouteMeters = 10_000.0
    )

    private fun isMatch(detourMeters: Double) = RouteMatchResult(
        matched = true,
        distanceFromRouteMeters = 900.0,
        directDistanceMeters = 20_000.0,
        viaPartnerDistanceMeters = 20_000.0 + detourMeters,
        detourMeters = detourMeters,
        partnerPositionOnRouteMeters = 8_000.0
    )

    private class FakeMatcher(
        var fn: (Location, Location, Location) -> RouteMatchResult = { _, _, _ -> throw IllegalStateException("no stub") }
    ) : RouteMatcher {
        var calls = 0
        override suspend fun match(
            origin: Location,
            destinationA: Location,
            destinationB: Location
        ): RouteMatchResult {
            calls++
            return fn(origin, destinationA, destinationB)
        }
    }

    private fun useCase(fake: FakeMatcher) =
        FindRouteMatchesUseCase(fake, RouteMatchConfig())

    @Test
    fun `offline keeps basic order with no route calls`() = runBlocking {
        val fake = FakeMatcher()
        val input = listOf(
            candidate("m", midtown, MatchLevel.SAME_CITY),
            candidate("i", itpl, MatchLevel.SAME_AREA)
        )

        val ranked = useCase(fake).rank(me, origin, input, networkAvailable = false)

        assertEquals(listOf("m", "i"), ranked.map { it.peer.endpointId })
        ranked.forEach {
            assertNull(it.routeLabel)
            assertTrue(!it.routeVerified)
        }
        assertEquals(0, fake.calls)
    }

    @Test
    fun `different city and far peers skip the route backend`() = runBlocking {
        val fake = FakeMatcher { _, _, _ -> isMatch(400.0) }
        val input = listOf(
            candidate("d", dwarka, MatchLevel.DIFFERENT),
            candidate("e", electronicCity, MatchLevel.SAME_CITY),
            candidate("i", itpl, MatchLevel.SAME_AREA)
        )

        val ranked = useCase(fake).rank(me, origin, input, networkAvailable = true)

        assertEquals(1, fake.calls)
        assertEquals("i", ranked.first().peer.endpointId)
        assertEquals("Going your way", ranked.first().routeLabel)
        assertTrue(ranked.drop(1).all { it.routeLabel == null })
    }

    @Test
    fun `verified route floats above basic same-area rank`() = runBlocking {
        val fake = FakeMatcher { _, _, b ->
            if (b.lng == midtown.location.lng) isMatch(2_000.0) else noMatch()
        }
        val input = listOf(
            candidate("i", itpl, MatchLevel.SAME_AREA),
            candidate("m", midtown, MatchLevel.SAME_CITY)
        )

        val ranked = useCase(fake).rank(me, origin, input, networkAvailable = true)

        assertEquals("m", ranked.first().peer.endpointId)
        assertEquals("Going your way · ~2 km detour", ranked.first().routeLabel)
        assertTrue(ranked.first().routeVerified)
        assertEquals(2, fake.calls)
    }

    @Test
    fun `matcher failure keeps basic rank`() = runBlocking {
        val fake = FakeMatcher { _, _, _ -> throw IllegalStateException("OSRM down") }
        val input = listOf(
            candidate("m", midtown, MatchLevel.SAME_CITY),
            candidate("i", itpl, MatchLevel.SAME_AREA)
        )

        val ranked = useCase(fake).rank(me, origin, input, networkAvailable = true)

        // No route verdicts: candidates keep their basic geographic rank
        // (same-area before same-city), with no labels.
        assertEquals(listOf("i", "m"), ranked.map { it.peer.endpointId })
        assertTrue(ranked.all { it.routeLabel == null && !it.routeVerified })
    }

    @Test
    fun `missing origin disables route checks`() = runBlocking {
        val fake = FakeMatcher { _, _, _ -> isMatch(100.0) }
        val input = listOf(candidate("i", itpl, MatchLevel.SAME_AREA))

        val ranked = useCase(fake).rank(me, null, input, networkAvailable = true)

        assertEquals(0, fake.calls)
        assertNull(ranked.single().routeLabel)
    }

    @Test
    fun `candidate cap bounds expensive checks`() = runBlocking {
        val fake = FakeMatcher { _, _, _ -> isMatch(100.0) }
        val input = (0 until 10).map { i ->
            val dest = Destination("Bengaluru", "Area $i", Location(12.9698 + 0.001 * i, 77.7499))
            candidate("p$i", dest, MatchLevel.SAME_AREA)
        }

        val ranked = useCase(fake).rank(me, origin, input, networkAvailable = true)

        assertEquals(10, ranked.size)
        assertEquals(RouteMatchConfig().maxRouteCandidates, fake.calls)
        assertEquals(8, ranked.count { it.routeVerified })
    }
}
