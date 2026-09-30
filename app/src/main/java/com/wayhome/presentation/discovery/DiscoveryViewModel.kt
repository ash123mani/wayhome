package com.wayhome.presentation.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayhome.core.ConnectivityObserver
import com.wayhome.core.IdentityManager
import com.wayhome.core.PermissionHelper
import com.wayhome.domain.matcher.DestinationMatcher
import com.wayhome.domain.model.MatchLevel
import com.wayhome.domain.model.Peer
import com.wayhome.domain.model.UserProfile
import com.wayhome.domain.repository.DiscoveryRepository
import com.wayhome.domain.routing.RouteOriginProvider
import com.wayhome.domain.usecase.FindRouteMatchesUseCase
import com.wayhome.domain.usecase.RouteCandidate
import com.wayhome.domain.usecase.RoutePeerRank
import com.wayhome.nearby.NearbyTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PeerUi(
    val peer: Peer,
    val level: MatchLevel,
    val distanceKm: Double?,
    val routeLabel: String? = null,
    val routeVerified: Boolean = false
)

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val discovery: DiscoveryRepository,
    private val identity: IdentityManager,
    private val transport: NearbyTransport,
    private val matcher: DestinationMatcher,
    private val routeMatches: FindRouteMatchesUseCase,
    private val routeOrigins: RouteOriginProvider,
    val permissions: PermissionHelper,
    private val connectivity: ConnectivityObserver
) : ViewModel() {
    val profile: StateFlow<UserProfile?> = identity.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val banner: StateFlow<String> = discovery.observeConnectionBanner()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "🔎 Searching nearby...")

    val internetNow: Boolean get() = connectivity.hasInternetNow()

    /** Existing offline geographic matching — behaviour unchanged. */
    private val basePeers: Flow<List<PeerUi>> = combine(
        discovery.observePeers(), identity.observeProfile()
    ) { peers, me ->
        peers.map { p ->
            val m = if (me == null) null else runCatching {
                matcher.calculateMatch(null, me.destination, p.destination)
            }.getOrNull()
            PeerUi(p, m?.level ?: MatchLevel.DIFFERENT, m?.distanceKm)
        }.sortedWith(baseOrder())
    }

    /**
     * Route verdicts, resolved off the main flow: debounced so peer churn
     * can't cause an OSRM storm, `mapLatest` so stale lookups are cancelled.
     * Any failure (offline, no origin, backend error) yields an empty map and
     * the UI keeps its basic geographic ranking.
     */
    private val routeRanks: StateFlow<Map<String, RoutePeerRank>> = basePeers
        .debounce(800)
        .mapLatest { base ->
            val me = profile.value
            if (me == null || base.isEmpty() || !connectivity.hasInternetNow()) {
                emptyMap()
            } else {
                val origin = routeOrigins.originFor(me.destination.city)
                    ?: return@mapLatest emptyMap<String, RoutePeerRank>()
                val candidates = base.map { RouteCandidate(it.peer, it.level, it.distanceKm) }
                runCatching {
                    routeMatches.rank(me.destination, origin, candidates, networkAvailable = true)
                }.getOrDefault(emptyList()).associateBy { it.peer.endpointId }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val peers: StateFlow<List<PeerUi>> = combine(basePeers, routeRanks) { base, ranks ->
        base.map { ui ->
            val rank = ranks[ui.peer.endpointId]
            if (rank?.routeVerified == true) {
                ui.copy(routeLabel = rank.routeLabel, routeVerified = true)
            } else ui
        }.sortedWith(
            compareBy<PeerUi> { if (it.routeVerified) 0 else 1 }.then(baseOrder())
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nearbyActive = MutableStateFlow(false)

    fun startSharing() {
        viewModelScope.launch {
            val me = identity.getOrCreate()
            transport.startAdvertising(me.tempId, me.destination, me.lookingForPartners)
            transport.startDiscovery()
            nearbyActive.value = true
        }
    }

    fun stopAll() {
        transport.stopAdvertising()
        transport.stopDiscovery()
        nearbyActive.value = false
    }

    fun connect(peer: Peer) {
        viewModelScope.launch { discovery.connect(peer) }
    }

    fun block(tempId: String) {
        viewModelScope.launch { discovery.blockUser(tempId) }
    }

    private fun baseOrder() = compareBy<PeerUi>(
        {
            if (it.level == MatchLevel.SAME_AREA) 0
            else if (it.level == MatchLevel.NEARBY_AREA) 1
            else if (it.level == MatchLevel.SAME_CITY) 2 else 3
        },
        { it.peer.tempId }
    )
}
