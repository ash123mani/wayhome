package com.wayhome.domain.routing

import com.wayhome.domain.model.Location
import com.wayhome.domain.repository.RouteRepository
import javax.inject.Inject
import kotlin.math.max

interface RouteMatcher {
    suspend fun match(
        origin: Location,
        destinationA: Location,
        destinationB: Location
    ): RouteMatchResult
}

/** Pure rule evaluation, unit-testable without any routing backend. */
internal fun decideRouteMatch(
    config: RouteMatchConfig,
    distanceFromRouteMeters: Double,
    detourMeters: Double,
    overshootBeyondDestinationMeters: Double
): RouteMatchStatus {
    if (distanceFromRouteMeters > config.maxDistanceFromRouteMeters) return RouteMatchStatus.NOT_A_MATCH
    if (!config.allowBeyondDestination &&
        overshootBeyondDestinationMeters > config.maxOvershootBeyondDestinationMeters
    ) return RouteMatchStatus.NOT_A_MATCH
    if (detourMeters > config.maxDetourMeters) return RouteMatchStatus.NOT_A_MATCH
    return RouteMatchStatus.MATCH
}

/**
 * OSRM-backed matcher. One routing call when B is clearly off-route
 * (lateral short-circuit), two when B is near A's route. Never throws:
 * backend failures become [RouteMatchStatus.UNAVAILABLE] (or NOT_A_MATCH
 * when [RouteMatchConfig.allowBasicFallback] is explicitly enabled).
 * Coroutine-safe: no shared mutable state (caching lives in the repository).
 */
class OsrmRouteMatcher @Inject constructor(
    private val routes: RouteRepository,
    private val config: RouteMatchConfig
) : RouteMatcher {
    override suspend fun match(
        origin: Location,
        destinationA: Location,
        destinationB: Location
    ): RouteMatchResult {
        if (!GeoUtils.isValid(origin) ||
            !GeoUtils.isValid(destinationA) ||
            !GeoUtils.isValid(destinationB)
        ) {
            return unavailable()
        }

        val direct = routes.drivingRoute(listOf(origin, destinationA)).getOrNull()
            ?.takeIf { it.distanceMeters.isFinite() && it.distanceMeters >= 0.0 && it.shape.size >= 2 }
            ?: return if (config.allowBasicFallback) notAMatchUnknown() else unavailable()

        val projection = GeoUtils.projectionOnRoute(direct.shape, destinationB)
            ?: return if (config.allowBasicFallback) {
                notAMatch(UNKNOWN_METERS, direct.distanceMeters, UNKNOWN_METERS)
            } else {
                unavailable()
            }

        if (projection.distanceFromRouteMeters > config.maxDistanceFromRouteMeters) {
            return notAMatch(
                projection.distanceFromRouteMeters, direct.distanceMeters, UNKNOWN_METERS,
                projection.positionOnRouteMeters
            )
        }
        if (!config.allowBeyondDestination &&
            projection.overshootBeyondEndMeters > config.maxOvershootBeyondDestinationMeters
        ) {
            return notAMatch(
                projection.distanceFromRouteMeters, direct.distanceMeters, UNKNOWN_METERS,
                projection.positionOnRouteMeters
            )
        }

        val via = routes.drivingRoute(listOf(origin, destinationB, destinationA)).getOrNull()
            ?.takeIf { it.distanceMeters.isFinite() && it.distanceMeters >= 0.0 }
            ?: return if (config.allowBasicFallback) {
                notAMatch(
                    projection.distanceFromRouteMeters, direct.distanceMeters, UNKNOWN_METERS,
                    projection.positionOnRouteMeters
                )
            } else {
                unavailable()
            }

        val detour = max(0.0, via.distanceMeters - direct.distanceMeters)
        val status = decideRouteMatch(
            config, projection.distanceFromRouteMeters, detour, projection.overshootBeyondEndMeters
        )
        return RouteMatchResult(
            matched = status == RouteMatchStatus.MATCH,
            distanceFromRouteMeters = projection.distanceFromRouteMeters,
            directDistanceMeters = direct.distanceMeters,
            viaPartnerDistanceMeters = via.distanceMeters,
            detourMeters = detour,
            partnerPositionOnRouteMeters = projection.positionOnRouteMeters,
            status = status
        )
    }

    private fun unavailable() = RouteMatchResult(
        matched = false,
        distanceFromRouteMeters = UNKNOWN_METERS,
        directDistanceMeters = UNKNOWN_METERS,
        viaPartnerDistanceMeters = UNKNOWN_METERS,
        detourMeters = UNKNOWN_METERS,
        partnerPositionOnRouteMeters = UNKNOWN_METERS,
        status = RouteMatchStatus.UNAVAILABLE
    )

    private fun notAMatchUnknown() = RouteMatchResult(
        matched = false,
        distanceFromRouteMeters = UNKNOWN_METERS,
        directDistanceMeters = UNKNOWN_METERS,
        viaPartnerDistanceMeters = UNKNOWN_METERS,
        detourMeters = UNKNOWN_METERS,
        partnerPositionOnRouteMeters = UNKNOWN_METERS,
        status = RouteMatchStatus.NOT_A_MATCH
    )

    private fun notAMatch(
        distanceFromRouteMeters: Double,
        directDistanceMeters: Double,
        viaPartnerDistanceMeters: Double,
        positionOnRouteMeters: Double = UNKNOWN_METERS
    ) = RouteMatchResult(
        matched = false,
        distanceFromRouteMeters = distanceFromRouteMeters,
        directDistanceMeters = directDistanceMeters,
        viaPartnerDistanceMeters = viaPartnerDistanceMeters,
        detourMeters = UNKNOWN_METERS,
        partnerPositionOnRouteMeters = positionOnRouteMeters,
        status = RouteMatchStatus.NOT_A_MATCH
    )
}
