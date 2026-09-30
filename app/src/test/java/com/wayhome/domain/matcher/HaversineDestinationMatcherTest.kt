package com.wayhome.domain.matcher

import com.wayhome.domain.model.Destination
import com.wayhome.domain.model.Location
import com.wayhome.domain.model.MatchLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HaversineDestinationMatcherTest {
    private val matcher = HaversineDestinationMatcher()

    @Test
    fun `same area within 2km matches SAME_AREA`() {
        val a = Destination("Bengaluru", "Whitefield", Location(12.9698, 77.7499))
        val b = Destination("Bengaluru", "ITPL", Location(12.9828, 77.7603))
        val result = matcher.calculateMatch(null, a, b)
        assertEquals(MatchLevel.SAME_AREA, result.level)
        assertTrue(result.goingSameWay)
    }

    @Test
    fun `far areas in same city match SAME_CITY but not same way`() {
        val a = Destination("Bengaluru", "Whitefield", Location(12.9698, 77.7499))
        val b = Destination("Bengaluru", "Electronic City", Location(12.8452, 77.6602))
        val result = matcher.calculateMatch(null, a, b)
        assertEquals(MatchLevel.SAME_CITY, result.level)
        assertTrue(!result.goingSameWay)
    }

    @Test
    fun `different cities match DIFFERENT`() {
        val a = Destination("Bengaluru", "Whitefield", Location(12.9698, 77.7499))
        val b = Destination("Delhi", "Dwarka", Location(28.5921, 77.0460))
        val result = matcher.calculateMatch(null, a, b)
        assertEquals(MatchLevel.DIFFERENT, result.level)
    }

    @Test
    fun `missing coordinates fall back to string match`() {
        val a = Destination("Bengaluru", "Whitefield")
        val b = Destination("Bengaluru", "Whitefield")
        assertEquals(MatchLevel.SAME_AREA, matcher.calculateMatch(null, a, b).level)
    }
}
