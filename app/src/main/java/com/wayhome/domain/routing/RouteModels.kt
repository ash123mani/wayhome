package com.wayhome.domain.routing

/**
 * Route-based matching domain. Works on the existing
 * [com.wayhome.domain.model.Location] / [com.wayhome.domain.model.Destination]
 * models — no duplicate coordinate types.
 */

/** Sentinel for "unknown", used instead of NaN/Infinity. Never expose to users. */
const val UNKNOWN_METERS = -1.0

/**
 * Routing outcome, kept separate from [NOT_A_MATCH]:
 * a failed lookup must never be presented as "not going your way".
 */
enum class RouteMatchStatus { MATCH, NOT_A_MATCH, UNAVAILABLE }

data class RouteMatchResult(
    val matched: Boolean,
    val distanceFromRouteMeters: Double,
    val directDistanceMeters: Double,
    val viaPartnerDistanceMeters: Double,
    val detourMeters: Double,
    /** Along-route distance from the origin to B's nearest point on A's route. */
    val partnerPositionOnRouteMeters: Double,
    val status: RouteMatchStatus = if (matched) RouteMatchStatus.MATCH else RouteMatchStatus.NOT_A_MATCH
) {
    init {
        require(
            (status == RouteMatchStatus.MATCH && matched) ||
                (status == RouteMatchStatus.NOT_A_MATCH && !matched) ||
                (status == RouteMatchStatus.UNAVAILABLE && !matched)
        ) { "matched=$matched is inconsistent with status=$status" }
    }
}

/**
 * Thresholds for "going the same way home". All configurable so product can
 * refine the rules later without touching matching logic.
 */
data class RouteMatchConfig(
    /** B must be within this distance of A's route. */
    val maxDistanceFromRouteMeters: Double = 2_000.0,
    /** Picking up B may add at most this much driving distance. */
    val maxDetourMeters: Double = 5_000.0,
    /** How far past A's destination B may lie along the route. */
    val maxOvershootBeyondDestinationMeters: Double = 1_000.0,
    /** When false (default), B lying beyond A is rejected regardless of detour. */
    val allowBeyondDestination: Boolean = false,
    /** Cheap pre-filter: skip route checks when A and B are further apart than this. */
    val maxCandidateRadiusKm: Double = 12.0,
    /** Cap on expensive OSRM-backed checks per ranking pass. */
    val maxRouteCandidates: Int = 8,
    /**
     * When true, routing failures degrade to NOT_A_MATCH instead of
     * UNAVAILABLE. Default false: failures stay visibly distinct so the UI
     * can fall back to basic geographic matching without lying.
     */
    val allowBasicFallback: Boolean = false
)
