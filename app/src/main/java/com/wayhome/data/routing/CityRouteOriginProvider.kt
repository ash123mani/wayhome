package com.wayhome.data.routing

import com.wayhome.domain.model.Location
import com.wayhome.domain.routing.RouteOriginProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeded journey origins. Bengaluru Airport is the reference origin from the
 * product spec; add further cities here (or load them from
 * assets/destinations.json) as the feature rolls out. Unknown cities return
 * null so route matching stays unavailable instead of guessing.
 */
@Singleton
class CityRouteOriginProvider @Inject constructor() : RouteOriginProvider {
    override fun originFor(city: String): Location? =
        ORIGINS[city.trim().lowercase()]

    companion object {
        private val ORIGINS: Map<String, Location> = mapOf(
            "bengaluru" to Location(lat = 12.9941, lng = 77.6163) // Kempegowda Intl Airport
        )
    }
}
