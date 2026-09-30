package com.wayhome.presentation.chat

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
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.components.AvatarRing
import com.wayhome.presentation.designsystem.components.ChatComposer
import com.wayhome.presentation.designsystem.components.EmptyState
import com.wayhome.presentation.designsystem.components.MessageBubble
import com.wayhome.presentation.designsystem.components.QuickReplies
import com.wayhome.presentation.designsystem.components.TravellerAvatar
import com.wayhome.presentation.designsystem.components.WayHomeButton
import com.wayhome.presentation.designsystem.components.WayHomeButtonStyle
import com.wayhome.presentation.designsystem.components.appearIn

private val travelOpeners = listOf(
    "Which terminal are you at?",
    "Where are you heading?",
    "Want to share a cab?",
    "Anyone else joining?"
)

@Composable
fun ChatScreen(
    endpointId: String,
    peerTempId: String,
    onBack: () -> Unit,
    onCreateGroup: () -> Unit,
    vm: ChatViewModel = hiltViewModel()
) {
    val c = WayHome.colors
    LaunchedEffect(endpointId, peerTempId) { vm.open(endpointId, peerTempId) }
    val messages by vm.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

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
        // Header: who you're talking to, in travel terms
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.surface)
                .padding(horizontal = Space.sm, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surfaceMuted)
                    .clickableNoRipple(onBack),
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
            TravellerAvatar(tempId = peerTempId, size = 40.dp, ring = AvatarRing.Connected)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(peerTempId, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                Text(
                    "Temporary chat · this trip only",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant
                )
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = Space.gutter),
                    verticalArrangement = Arrangement.Center
                ) {
                    EmptyState(
                        glyph = "👋",
                        title = "Say hello to $peerTempId",
                        body = "Ask where they're heading, or whether they want to share a cab. " +
                            "This chat works without internet."
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Space.gutter, end = Space.gutter,
                        top = Space.lg, bottom = Space.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    items(messages, key = { it.id }) { m ->
                        val mine = m.senderId != peerTempId
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
                QuickReplies(
                    suggestions = travelOpeners,
                    onPick = { input = it }
                )
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
            Spacer(Modifier.height(Space.sm))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "✈️ Works offline — no internet needed",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.quiet
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickableNoRipple(onCreateGroup)
                        .padding(horizontal = Space.sm, vertical = Space.xs)
                ) {
                    Text(
                        "Make a group",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.accent
                    )
                }
            }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
