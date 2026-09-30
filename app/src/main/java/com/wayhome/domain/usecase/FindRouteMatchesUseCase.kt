package com.wayhome.domain.usecase

import com.wayhome.domain.model.Destination
import com.wayhome.domain.model.Location
import com.wayhome.domain.model.MatchLevel
import com.wayhome.domain.model.Peer
import com.wayhome.domain.routing.GeoUtils
import com.wayhome.domain.routing.RouteMatchConfig
import com.wayhome.domain.routing.RouteMatchStatus
import com.wayhome.domain.routing.RouteMatcher
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

/** One nearby traveller with its existing geographic match level. */
data class RouteCandidate(
    val peer: Peer,
    val level: MatchLevel,
    val distanceKm: Double?
)

/**
 * A candidate plus its route verdict. [routeLabel] is user-facing copy only
 * ("Going your way") — raw meter values never leave the domain layer.
 */
data class RoutePeerRank(
    val peer: Peer,
    val level: MatchLevel,
    val distanceKm: Double?,
    val routeLabel: String?,
    val routeVerified: Boolean,
    val detourMeters: Double?
)

/**
 * Pipeline: nearby users → same city → cheap haversine filter → top
 * candidates → OSRM route matching → ranked list. Never throws; on any
 * failure (no network, no origin, backend error) every candidate keeps its
 * basic geographic rank with no route label.
 */
class FindRouteMatchesUseCase @Inject constructor(
    private val matcher: RouteMatcher,
    private val config: RouteMatchConfig
) {
    suspend fun rank(
        myDestination: Destination,
        origin: Location?,
        candidates: List<RouteCandidate>,
        networkAvailable: Boolean
    ): List<RoutePeerRank> {
        val base = candidates.map {
            RoutePeerRank(it.peer, it.level, it.distanceKm, null, false, null)
        }
        if (!networkAvailable || candidates.isEmpty()) return base
        if (origin == null || !GeoUtils.isValid(origin)) return base
        val myLocation = myDestination.location
        if (!GeoUtils.isValid(myLocation)) return base

        val eligible = candidates.mapNotNull { candidate ->
            if (!sameCity(myDestination.city, candidate.peer.destination.city)) return@mapNotNull null
            val peerLocation = candidate.peer.destination.location
            if (!GeoUtils.isValid(peerLocation)) return@mapNotNull null
            val abMeters = GeoUtils.distanceBetween(myLocation, peerLocation)
            if (abMeters < 0.0 || abMeters > config.maxCandidateRadiusKm * 1_000.0) return@mapNotNull null
            candidate to abMeters
        }.sortedBy { it.second }.take(config.maxRouteCandidates)
        if (eligible.isEmpty()) return base

        val verdicts = supervisorScope {
            eligible.map { (candidate, _) ->
                async {
                    val result = runCatching {
                        matcher.match(origin, myLocation, candidate.peer.destination.location)
                    }.getOrNull()
                    candidate.peer.endpointId to result
                }
            }.awaitAll().toMap()
        }

        val ranked = base.associateBy { it.peer.endpointId }.toMutableMap()
        verdicts.forEach { (endpointId, result) ->
            val current = ranked[endpointId] ?: return@forEach
            if (result != null && result.status == RouteMatchStatus.MATCH && result.matched) {
                ranked[endpointId] = current.copy(
                    routeLabel = routeLabel(result.detourMeters),
                    routeVerified = true,
                    detourMeters = result.detourMeters.takeIf { it >= 0.0 }
                )
            }
        }
        return ranked.values.sortedWith(
            compareBy<RoutePeerRank> { if (it.routeVerified) 0 else 1 }
                .thenBy { it.detourMeters ?: Double.MAX_VALUE }
                .thenBy { levelRank(it.level) }
                .thenBy { it.peer.tempId }
        )
    }

    private fun sameCity(a: String, b: String): Boolean =
        a.isNotBlank() && a.equals(b, ignoreCase = true)

    private fun levelRank(level: MatchLevel): Int = when (level) {
        MatchLevel.SAME_AREA -> 0
        MatchLevel.NEARBY_AREA -> 1
        MatchLevel.SAME_CITY -> 2
        MatchLevel.DIFFERENT -> 3
    }

    private fun routeLabel(detourMeters: Double): String {
        if (detourMeters >= DETOUR_LABEL_THRESHOLD_METERS) {
            val km = max(1, (detourMeters / 1_000.0).roundToInt())
            return "Going your way · ~$km km detour"
        }
        return "Going your way"
    }

    companion object {
        const val DETOUR_LABEL_THRESHOLD_METERS = 1_500.0
    }
}
