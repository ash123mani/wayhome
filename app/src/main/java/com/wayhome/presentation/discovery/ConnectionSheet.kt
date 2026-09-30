package com.wayhome.presentation.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wayhome.domain.model.MatchLevel
import com.wayhome.domain.model.Peer
import com.wayhome.domain.model.PeerConnectionState
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.WayHomeTravelType
import com.wayhome.presentation.designsystem.components.Eyebrow
import com.wayhome.presentation.designsystem.components.MatchPath
import com.wayhome.presentation.designsystem.components.StatusPill
import com.wayhome.presentation.designsystem.components.StatusTone
import com.wayhome.presentation.designsystem.components.TravellerAvatar
import com.wayhome.presentation.designsystem.components.WayHomeButton
import com.wayhome.presentation.designsystem.components.WayHomeButtonStyle
import com.wayhome.presentation.designsystem.components.AvatarRing

/**
 * The connection moment, in human terms: "you're both heading towards
 * Whitefield" — not "connection request sent".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSheet(
    peer: Peer,
    myArea: String,
    onSayHello: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = WayHome.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sameArea = peer.destination.area.equals(myArea, ignoreCase = true)
    val headline = if (sameArea) {
        "You're both heading towards ${peer.destination.area}."
    } else {
        "You're both near ${peer.destination.area}."
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.background,
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().padding(top = Space.md),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.outlineStrong)
                )
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TravellerAvatar(
                    tempId = peer.tempId,
                    size = 60.dp,
                    ring = if (peer.connectionState == PeerConnectionState.CONNECTED)
                        AvatarRing.Connected else AvatarRing.Searching
                )
                Spacer(Modifier.width(Space.lg))
                Column(Modifier.weight(1f)) {
                    Eyebrow(peer.tempId)
                    Spacer(Modifier.height(Space.xxs))
                    Text(
                        peer.destination.label(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.onSurface
                    )
                }
                if (peer.lookingForPartners) {
                    StatusPill(label = "Free to join", tone = StatusTone.Good)
                }
            }

            Spacer(Modifier.height(Space.xl))
            Text(headline, style = MaterialTheme.typography.titleLarge, color = c.onSurface)
            Spacer(Modifier.height(Space.lg))
            MatchPath(
                myArea = myArea,
                theirArea = peer.destination.area,
                level = if (sameArea) MatchLevel.SAME_AREA else MatchLevel.NEARBY_AREA
            )

            Spacer(Modifier.height(Space.xl))
            Text(
                "Say hello and find out if you're really on the same route. " +
                    "You only see each other's destination area — never a phone number or address.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant
            )

            Spacer(Modifier.height(Space.xl))
            WayHomeButton(
                text = "Say hello 👋",
                onClick = onSayHello,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Space.sm))
            WayHomeButton(
                text = "Not now",
                onClick = onDismiss,
                style = WayHomeButtonStyle.Ghost,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Space.section))
        }
    }
}
