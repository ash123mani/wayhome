package com.wayhome.domain.routing

import com.wayhome.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {
    private fun loc(lat: Double?, lng: Double?) = Location(lat, lng)

    @Test
    fun `same point has zero distance`() {
        assertEquals(0.0, GeoUtils.distanceBetween(loc(12.9, 77.7), loc(12.9, 77.7)), 0.0)
    }

    @Test
    fun `one degree of latitude is about 111km`() {
        assertEquals(111_194.9, GeoUtils.distanceBetween(loc(0.0, 0.0), loc(1.0, 0.0)), 1.0)
    }

    @Test
    fun `nearby and far points scale sensibly`() {
        val near = GeoUtils.distanceBetween(loc(12.9698, 77.7499), loc(12.9828, 77.7603))
        assertTrue(near in 1_000.0..3_000.0)
        val far = GeoUtils.distanceBetween(loc(12.9698, 77.7499), loc(28.5921, 77.0460))
        assertTrue(far > 1_500_000.0)
    }

    @Test
    fun `point directly on segment has zero distance`() {
        val d = GeoUtils.distanceToSegment(loc(0.0, 0.05), loc(0.0, 0.0), loc(0.0, 0.1))
        assertEquals(0.0, d, 1.0)
    }

    @Test
    fun `point 1km from segment measures about 1km`() {
        // 0.009 degrees of latitude ~ 1000.7 m.
        val d = GeoUtils.distanceToSegment(loc(0.009, 0.05), loc(0.0, 0.0), loc(0.0, 0.1))
        assertEquals(1_000.7, d, 3.0)
    }

    @Test
    fun `point 2km from segment measures about 2km`() {
        val d = GeoUtils.distanceToSegment(loc(0.018, 0.05), loc(0.0, 0.0), loc(0.0, 0.1))
        assertEquals(2_001.5, d, 4.0)
    }

    @Test
    fun `point beyond 2km from segment exceeds threshold`() {
        val d = GeoUtils.distanceToSegment(loc(0.025, 0.05), loc(0.0, 0.0), loc(0.0, 0.1))
        assertTrue(d > 2_000.0)
    }

    @Test
    fun `zero-length segment degrades to endpoint distance`() {
        val expected = GeoUtils.distanceBetween(loc(0.01, 0.02), loc(0.0, 0.0))
        assertEquals(expected, GeoUtils.distanceToSegment(loc(0.01, 0.02), loc(0.0, 0.0), loc(0.0, 0.0)), 0.0)
    }

    @Test
    fun `invalid coordinates yield unknown sentinel, never NaN or infinity`() {
        val bad = listOf(
            GeoUtils.distanceBetween(loc(null, 0.0), loc(0.0, 0.0)),
            GeoUtils.distanceBetween(loc(Double.NaN, 0.0), loc(0.0, 0.0)),
            GeoUtils.distanceBetween(loc(0.0, 0.0), loc(91.0, 0.0)),
            GeoUtils.distanceBetween(loc(0.0, 0.0), loc(0.0, 181.0)),
            GeoUtils.distanceBetween(null, loc(0.0, 0.0)),
            GeoUtils.distanceToSegment(loc(Double.POSITIVE_INFINITY, 0.0), loc(0.0, 0.0), loc(0.0, 0.1))
        )
        bad.forEach {
            assertEquals(UNKNOWN_METERS, it, 0.0)
            assertTrue(it.isFinite())
        }
        assertFalse(GeoUtils.isValid(loc(null, null)))
    }

    @Test
    fun `projection reports position along route`() {
        val shape = listOf(loc(0.0, 0.0), loc(0.0, 0.1))
        val projection = GeoUtils.projectionOnRoute(shape, loc(0.0, 0.05))!!
        assertEquals(0.0, projection.distanceFromRouteMeters, 1.0)
        assertEquals(5_559.7, projection.positionOnRouteMeters, 5.0)
        assertEquals(0, projection.segmentIndex)
        assertEquals(0.0, projection.overshootBeyondEndMeters, 0.0)
        assertEquals(11_119.5, projection.routeLengthMeters, 5.0)
    }

    @Test
    fun `projection past the final endpoint reports overshoot`() {
        val shape = listOf(loc(0.0, 0.0), loc(0.0, 0.1))
        val projection = GeoUtils.projectionOnRoute(shape, loc(0.0, 0.12))!!
        assertTrue(projection.overshootBeyondEndMeters > 2_000.0)
    }

    @Test
    fun `projection rejects empty or invalid shapes`() {
        assertNull(GeoUtils.projectionOnRoute(emptyList(), loc(0.0, 0.0)))
        assertNull(GeoUtils.projectionOnRoute(listOf(loc(0.0, 0.0)), loc(0.0, 0.0)))
        assertNull(GeoUtils.projectionOnRoute(listOf(loc(0.0, 0.0), loc(null, 0.0)), loc(0.0, 0.0)))
        assertNull(GeoUtils.projectionOnRoute(listOf(loc(0.0, 0.0), loc(0.0, 0.1)), loc(null, 0.0)))
    }
}
