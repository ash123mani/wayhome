package com.wayhome.presentation.discovery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayhome.domain.model.MatchLevel
import com.wayhome.domain.model.Peer
import com.wayhome.domain.model.PeerConnectionState
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.components.EmptyState
import com.wayhome.presentation.designsystem.components.Eyebrow
import com.wayhome.presentation.designsystem.components.NearbyClusterCard
import com.wayhome.presentation.designsystem.components.OfflineBanner
import com.wayhome.presentation.designsystem.components.RouteTicket
import com.wayhome.presentation.designsystem.components.SearchAnimation
import com.wayhome.presentation.designsystem.components.SectionHeader
import com.wayhome.presentation.designsystem.components.StatusIndicator
import com.wayhome.presentation.designsystem.components.StatusTone
import com.wayhome.presentation.designsystem.components.TravellerCard
import com.wayhome.presentation.designsystem.components.WayHomeButton
import com.wayhome.presentation.designsystem.components.WayHomeButtonStyle
import com.wayhome.presentation.designsystem.components.appearIn
import java.util.Calendar

/**
 * Home. One question — "who around me is going my way?" — answered by a live
 * scan, a nearby cluster, then one card per traveller ordered by route overlap.
 */
@Composable
fun DiscoveryScreen(
    onOpenChat: (endpointId: String, tempId: String) -> Unit,
    onOpenGroup: () -> Unit,
    onOpenProfile: () -> Unit,
    onChangeDestination: () -> Unit,
    vm: DiscoveryViewModel = hiltViewModel()
) {
    val c = WayHome.colors
    val peers by vm.peers.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val nearbyActive by vm.nearbyActive.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var asked by remember { mutableStateOf(false) }
    var helloTarget by remember { mutableStateOf<Peer?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.startSharing() }

    LaunchedEffect(Unit) {
        if (!asked) {
            asked = true
            val missing = vm.permissions.missingPermissions()
            if (missing.isEmpty()) vm.startSharing() else launcher.launch(missing.toTypedArray())
        }
    }

    val myArea = profile?.destination?.area.orEmpty()
    val myCity = profile?.destination?.city.orEmpty()
    val connectedCount = peers.count { it.peer.connectionState == PeerConnectionState.CONNECTED }
    val sameWay = peers.filter {
        it.level == MatchLevel.SAME_AREA || it.level == MatchLevel.NEARBY_AREA || it.routeVerified
    }
    val closest = sameWay.firstOrNull()?.peer ?: peers.firstOrNull()?.peer

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.skyTop, c.background)))
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = Space.gutter, end = Space.gutter,
            top = Space.lg, bottom = Space.section
        ),
        verticalArrangement = Arrangement.spacedBy(Space.lg)
    ) {
        item {
            GreetingHeader(
                tempId = profile?.tempId,
                city = myCity,
                area = myArea,
                distanceKm = sameWay.firstOrNull()?.distanceKm ?: peers.firstOrNull()?.distanceKm,
                caption = if (peers.isEmpty()) "Searching your area"
                else "${sameWay.size} going your way",
                onChangeDestination = onChangeDestination
            )
        }

        item {
            Column {
                OfflineBanner(internetAvailable = vm.internetNow)
                Spacer(Modifier.height(Space.sm))
                Text(
                    bannerText(peers.size, nearbyActive),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.quiet
                )
            }
        }

        if (peers.isEmpty()) {
            item { SearchingEmptyState(onKeepSearching = { vm.startSharing() }) }
        } else {
            item {
                NearbyClusterCard(
                    tempIds = peers.map { it.peer.tempId },
                    areas = peers.map { it.peer.destination.area },
                    actionLabel = "Say hello to your closest match",
                    onAction = { closest?.let { helloTarget = it } }
                )
            }

            item {
                SectionHeader(
                    title = if (sameWay.isNotEmpty())
                        "Going your way · ${sameWay.size}" else "Nearby travellers",
                    trailing = {
                        if (connectedCount > 0) {
                            StatusIndicator("$connectedCount connected", StatusTone.Good)
                        }
                    }
                )
            }

            itemsIndexed(peers, key = { _, ui -> ui.peer.endpointId }) { index, ui ->
                TravellerCard(
                    peer = ui.peer,
                    level = ui.level,
                    myArea = myArea,
                    distanceKm = ui.distanceKm,
                    routeLabel = ui.routeLabel,
                    routeVerified = ui.routeVerified,
                    onSayHello = { helloTarget = ui.peer },
                    onBlock = { vm.block(ui.peer.tempId) },
                    modifier = Modifier.appearIn(index * 70)
                )
            }

            item {
                WayHomeButton(
                    text = "Create a ride group",
                    onClick = onOpenGroup,
                    style = WayHomeButtonStyle.Outline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    AnimatedVisibility(visible = helloTarget != null, enter = fadeIn(), exit = fadeOut()) {
        helloTarget?.let { peer ->
            ConnectionSheet(
                peer = peer,
                myArea = myArea,
                onSayHello = {
                    vm.connect(peer)
                    helloTarget = null
                    onOpenChat(peer.endpointId, peer.tempId)
                },
                onDismiss = { helloTarget = null }
            )
        }
    }
}

@Composable
private fun SearchingEmptyState(onKeepSearching: () -> Unit) {
    val c = WayHome.colors
    WayHomeCardBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SearchAnimation(found = 0, diameter = 190.dp)
            EmptyState(
                glyph = "✈",
                title = "No travellers yet",
                body = "You might be the first person looking for a ride this way. " +
                    "Keep searching — someone may appear soon.",
                action = {
                    WayHomeButton(
                        text = "Keep searching",
                        onClick = onKeepSearching,
                        style = WayHomeButtonStyle.Tonal
                    )
                }
            )
        }
    }
}

@Composable
private fun WayHomeCardBox(content: @Composable () -> Unit) {
    val c = WayHome.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun GreetingHeader(
    tempId: String?,
    city: String,
    area: String,
    distanceKm: Double?,
    caption: String,
    onChangeDestination: () -> Unit
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello"
    }
    Column {
        Eyebrow(if (tempId != null) "$greeting · $tempId" else greeting)
        Spacer(Modifier.height(Space.sm))
        Text(
            "Where are you heading?",
            style = MaterialTheme.typography.displayMedium,
            color = WayHome.colors.onSurface
        )
        Spacer(Modifier.height(Space.lg))
        RouteTicket(
            city = city,
            area = area,
            distanceKm = distanceKm,
            caption = caption,
            onClick = onChangeDestination
        )
    }
}

private fun bannerText(count: Int, active: Boolean): String = when {
    count == 0 && active -> "Looking around you…"
    count == 0 -> "You're discoverable — waiting for travellers nearby"
    count == 1 -> "1 traveller found"
    else -> "$count travellers found"
}
