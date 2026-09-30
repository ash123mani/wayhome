package com.wayhome.domain.routing

import com.wayhome.domain.model.Location

/**
 * Journey origin per city (e.g. the airport travellers are heading home
 * from). Null means "no known origin" — route matching is then unavailable
 * and the app keeps its existing geographic matching.
 */
interface RouteOriginProvider {
    fun originFor(city: String): Location?
}
