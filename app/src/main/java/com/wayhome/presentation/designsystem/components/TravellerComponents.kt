package com.wayhome.presentation.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wayhome.domain.model.MatchLevel
import com.wayhome.domain.model.Peer
import com.wayhome.domain.model.PeerConnectionState
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome

/**
 * A person, not a profile: temporary name, where they're headed, how that compares
 * to your own route, and whether they're looking for partners. Nothing technical.
 */
@Composable
fun TravellerCard(
    peer: Peer,
    level: MatchLevel,
    myArea: String,
    distanceKm: Double?,
    onSayHello: () -> Unit,
    onBlock: () -> Unit,
    modifier: Modifier = Modifier,
    routeLabel: String? = null,
    routeVerified: Boolean = false
) {
    val c = WayHome.colors
    val connected = peer.connectionState == PeerConnectionState.CONNECTED
    val connecting = peer.connectionState == PeerConnectionState.CONNECTING

    val container by animateColorAsState(
        if (connected) c.surface else c.surface,
        tween(300),
        label = "cardBg"
    )

    WayHomeCard(modifier.fillMaxWidth(), container = container) {
        Column(Modifier.padding(Space.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TravellerAvatar(
                    tempId = peer.tempId,
                    size = 52.dp,
                    ring = when {
                        connected -> AvatarRing.Connected
                        connecting -> AvatarRing.Searching
                        else -> AvatarRing.None
                    }
                )
                Spacer(Modifier.width(Space.lg))
                Column(Modifier.weight(1f)) {
                    Text(
                        peer.tempId,
                        style = MaterialTheme.typography.titleLarge,
                        color = c.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (connected) "Connected · in touch" else "Nearby you right now",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant
                    )
                }
                if (peer.lookingForPartners) {
                    StatusIndicator(label = "Free to join", tone = StatusTone.Good)
                }
            }

            Spacer(Modifier.height(Space.xl))
            if (routeLabel != null) {
                StatusIndicator(
                    label = routeLabel,
                    tone = if (routeVerified) StatusTone.Good else StatusTone.Pending
                )
                Spacer(Modifier.height(Space.md))
            }
            MatchPath(myArea = myArea, theirArea = peer.destination.area, level = level)

            if (distanceKm != null && level == MatchLevel.NEARBY_AREA) {
                Spacer(Modifier.height(Space.xs))
                Text(
                    "About ${"%.1f".format(distanceKm)} km apart on the map",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.quiet
                )
            }

            Spacer(Modifier.height(Space.xl))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WayHomeButton(
                    text = if (connected) "Open chat" else "Say hello",
                    onClick = onSayHello,
                    style = if (connected) WayHomeButtonStyle.Tonal else WayHomeButtonStyle.Primary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Space.sm))
                Text(
                    "Block",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.quiet,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onBlock)
                        .padding(horizontal = Space.md, vertical = Space.md)
                )
            }
        }
    }
}

/** "4 people nearby" cluster summary with a single way in. */
@Composable
fun NearbyClusterCard(
    tempIds: List<String>,
    areas: List<String>,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = "See who's going your way"
) {
    val c = WayHome.colors
    WayHomeCard(modifier.fillMaxWidth(), container = c.accentSoft, border = Color.Transparent) {
        Column(Modifier.padding(Space.xl)) {
            StatusIndicator(
                label = "${tempIds.size} ${if (tempIds.size == 1) "person" else "people"} nearby",
                tone = StatusTone.Good
            )
            Spacer(Modifier.height(Space.lg))
            AvatarStack(tempIds = tempIds, size = 34.dp)
            Spacer(Modifier.height(Space.lg))
            Text(
                areas.distinct().take(3).joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
                color = c.onAccentSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Heading to ${areas.distinct().size} " +
                    "${if (areas.distinct().size == 1) "area" else "areas"} near you",
                style = MaterialTheme.typography.bodySmall,
                color = c.onAccentSoft
            )
            Spacer(Modifier.height(Space.lg))
            WayHomeButton(
                text = actionLabel,
                onClick = onAction,
                style = WayHomeButtonStyle.Primary
            )
        }
    }
}

private val TravellerListSpacing = Space.md

/* ------------------------------------------------------------------- groups */

@Composable
fun GroupHeader(
    name: String,
    members: List<String>,
    myTempId: String,
    modifier: Modifier = Modifier,
    onLeave: (() -> Unit)? = null
) {
    val c = WayHome.colors
    Column(modifier.fillMaxWidth()) {
        Text(name, style = MaterialTheme.typography.headlineMedium, color = c.onSurface)
        Spacer(Modifier.height(Space.xs))
        Text(
            "${members.size} ${if (members.size == 1) "traveller" else "travellers"} · Temporary group",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant
        )
        Spacer(Modifier.height(Space.lg))
        WayHomeCard(container = c.surface) {
            Column(Modifier.padding(Space.lg)) {
                members.forEach { member ->
                    val isMe = member == myTempId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = Space.sm)
                            .semantics {
                                contentDescription =
                                    if (isMe) "$member, you" else "$member, ${
                                        if (member == myTempId) "you" else "traveller"
                                    }"
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TravellerAvatar(tempId = member, size = 32.dp)
                        Spacer(Modifier.width(Space.md))
                        Text(
                            if (isMe) "You" else member,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isMe) c.accent else c.onSurface
                        )
                    }
                }
            }
        }
        if (onLeave != null) {
            Spacer(Modifier.height(Space.md))
            Text(
                "Leave group",
                style = MaterialTheme.typography.labelMedium,
                color = c.quiet,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onLeave)
                    .padding(horizontal = Space.md, vertical = Space.md)
            )
        }
    }
}

/** Confirmation offered after a match: "3 people are heading towards X." */
@Composable
fun GroupOfferCard(
    candidateNames: List<String>,
    areaLabel: String,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = WayHome.colors
    WayHomeCard(modifier.fillMaxWidth(), container = c.surface) {
        Column(Modifier.padding(Space.xl)) {
            Text(
                "${candidateNames.size} ${if (candidateNames.size == 1) "person is" else "people are"} heading towards $areaLabel.",
                style = MaterialTheme.typography.titleLarge,
                color = c.onSurface
            )
            Spacer(Modifier.height(Space.lg))
            AvatarStack(tempIds = candidateNames, size = 32.dp)
            Spacer(Modifier.height(Space.lg))
            candidateNames.forEach { name ->
                Text(
                    "· $name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Space.xs))
            Text(
                "· You",
                style = MaterialTheme.typography.bodyMedium,
                color = c.accent
            )
            Spacer(Modifier.height(Space.xl))
            WayHomeButton(text = "Create ride group", onClick = onCreate)
        }
    }
}
