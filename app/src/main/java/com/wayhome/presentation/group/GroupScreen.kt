package com.wayhome.presentation.group

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayhome.domain.model.MatchLevel
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.components.Eyebrow
import com.wayhome.presentation.designsystem.components.ChatComposer
import com.wayhome.presentation.designsystem.components.EmptyState
import com.wayhome.presentation.designsystem.components.GroupHeader
import com.wayhome.presentation.designsystem.components.GroupOfferCard
import com.wayhome.presentation.designsystem.components.MessageBubble
import com.wayhome.presentation.designsystem.components.QuickReplies
import com.wayhome.presentation.designsystem.components.StatusIndicator
import com.wayhome.presentation.designsystem.components.StatusTone
import com.wayhome.presentation.designsystem.components.SystemNote
import com.wayhome.presentation.designsystem.components.WayHomeButton
import com.wayhome.presentation.designsystem.components.WayHomeButtonStyle
import com.wayhome.presentation.designsystem.components.WayHomeCard

private val groupOpeners = listOf(
    "Where should we meet?",
    "Which side of the terminal?",
    "Sharing a cab — who's in?",
    "Leaving in 15 minutes"
)

/** Groups tab: temporary ride crews you can start or open. */
@Composable
fun GroupListScreen(
    onOpenGroup: (String) -> Unit,
    vm: GroupViewModel = hiltViewModel()
) {
    val c = WayHome.colors
    val groups by vm.groups.collectAsStateWithLifecycle()
    val peers by vm.peers.collectAsStateWithLifecycle()
    val myId by vm.myId.collectAsStateWithLifecycle()

    val candidates = peers.take(3)
    val areaLabel = candidates.firstOrNull()?.destination?.area
        ?: groups.firstOrNull()?.name?.substringAfter("Ride Group", "Ride Group")?.trim()
        ?: "your area"

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = Space.gutter, end = Space.gutter,
            top = Space.lg, bottom = Space.section
        ),
        verticalArrangement = Arrangement.spacedBy(Space.lg)
    ) {
        item {
            Column {
                Eyebrow("Share a cab", color = c.accent)
                Spacer(Modifier.height(Space.sm))
                Text("Ride groups", style = MaterialTheme.typography.displayMedium, color = c.onSurface)
                Spacer(Modifier.height(Space.xs))
                Text(
                    "Temporary crews for one trip. Nothing is saved after you leave.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
            }
        }

        if (candidates.isNotEmpty()) {
            item {
                GroupOfferCard(
                    candidateNames = candidates.map { it.tempId },
                    areaLabel = areaLabel,
                    onCreate = { vm.create("$areaLabel Ride Group") }
                )
            }
        } else {
            item {
                EmptyState(
                    glyph = "🧑‍🤝‍🧑",
                    title = "No one to group up with yet",
                    body = "When travellers going your way appear, start a group here and coordinate the cab together."
                )
            }
        }

        items(groups, key = { it.groupId }) { g ->
            WayHomeCard(Modifier.fillMaxWidth(), onClick = { onOpenGroup(g.groupId) }) {
                Column(Modifier.padding(Space.xl)) {
                    Text(g.name, style = MaterialTheme.typography.titleLarge, color = c.onSurface)
                    Spacer(Modifier.height(Space.xs))
                    StatusIndicator(
                        label = "${g.members.size} ${if (g.members.size == 1) "traveller" else "travellers"} · temporary",
                        tone = if (g.members.size > 1) StatusTone.Good else StatusTone.Quiet
                    )
                    Spacer(Modifier.height(Space.lg))
                    Text(
                        g.members.joinToString(" · ") { if (it == myId) "You" else it },
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Group detail: members, then the crew chat. */
@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit,
    vm: GroupViewModel = hiltViewModel()
) {
    val c = WayHome.colors
    LaunchedEffect(groupId) { vm.select(groupId) }
    val groups by vm.groups.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val myId by vm.myId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val group = groups.firstOrNull { it.groupId == groupId }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.sm, vertical = Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surfaceMuted)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = c.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(Space.sm))
            Text(
                group?.name ?: "Group",
                style = MaterialTheme.typography.titleLarge,
                color = c.onSurface
            )
        }

        if (group == null) {
            EmptyState(
                glyph = "🧑‍🤝‍🧑",
                title = "This group has ended",
                body = "Groups are temporary. Head back and start a new one with travellers nearby."
            )
            Spacer(Modifier.weight(1f))
        } else {
            Column(Modifier.padding(horizontal = Space.gutter)) {
                GroupHeader(
                    name = group.name,
                    members = group.members,
                    myTempId = myId,
                    onLeave = { vm.leave() }
                )
            }
            Spacer(Modifier.height(Space.lg))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = Space.gutter),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyState(
                            glyph = "💬",
                            title = "Chat about your ride",
                            body = "Agree on a meeting point, how many seats you need, and when to leave."
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Space.gutter, end = Space.gutter, bottom = Space.lg
                        ),
                        verticalArrangement = Arrangement.spacedBy(Space.md)
                    ) {
                        item { SystemNote("Group created · temporary") }
                        items(messages, key = { it.id }) { m ->
                            val mine = m.senderId == myId
                            MessageBubble(
                                text = m.text,
                                timestamp = m.timestamp,
                                mine = mine,
                                senderName = if (mine) null else m.senderId
                            )
                        }
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = Space.gutter, vertical = Space.md)
            ) {
                if (messages.isEmpty()) {
                    QuickReplies(suggestions = groupOpeners, onPick = { input = it })
                    Spacer(Modifier.height(Space.md))
                }
                ChatComposer(
                    value = input,
                    onValueChange = { input = it },
                    onSend = {
                        vm.send(input)
                        input = ""
                    }
                )
            }
        }
    }
}
