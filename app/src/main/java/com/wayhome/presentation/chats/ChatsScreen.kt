package com.wayhome.presentation.chats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.wayhome.domain.model.PeerConnectionState
import com.wayhome.domain.model.RideGroup
import com.wayhome.domain.repository.ChatRepository
import com.wayhome.domain.repository.DiscoveryRepository
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.components.Eyebrow
import com.wayhome.presentation.designsystem.components.AvatarRing
import com.wayhome.presentation.designsystem.components.EmptyState
import com.wayhome.presentation.designsystem.components.StatusIndicator
import com.wayhome.presentation.designsystem.components.StatusTone
import com.wayhome.presentation.designsystem.components.TravellerAvatar
import com.wayhome.presentation.designsystem.components.WayHomeCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ConversationRow(
    val key: String,
    val title: String,
    val subtitle: String,
    val kind: Kind,
    val endpointId: String = "",
    val tempId: String = ""
) {
    enum class Kind { Direct, Group }
}

@HiltViewModel
class ChatsViewModel @Inject constructor(
    discovery: DiscoveryRepository,
    chat: ChatRepository
) : ViewModel() {
    val rows: StateFlow<List<ConversationRow>> =
        combine(
            discovery.observePeers(),
            chat.observeGroups()
        ) { peers, groups ->
            val direct = peers
                .filter { it.connectionState == PeerConnectionState.CONNECTED }
                .map {
                    ConversationRow(
                        key = "peer:${it.endpointId}",
                        title = it.tempId,
                        subtitle = "Going to ${it.destination.area.ifBlank { it.destination.city }}",
                        kind = ConversationRow.Kind.Direct,
                        endpointId = it.endpointId,
                        tempId = it.tempId
                    )
                }
            val groupRows = groups.map {
                ConversationRow(
                    key = "group:${it.groupId}",
                    title = it.name,
                    subtitle = "${it.members.size} travellers · temporary group",
                    kind = ConversationRow.Kind.Group,
                    tempId = it.groupId
                )
            }
            groupRows + direct
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun ChatsScreen(
    onOpenDirect: (endpointId: String, tempId: String) -> Unit,
    onOpenGroup: (String) -> Unit,
    vm: ChatsViewModel = hiltViewModel()
) {
    val c = WayHome.colors
    val rows by vm.rows.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = Space.gutter, end = Space.gutter,
            top = Space.lg, bottom = Space.section
        ),
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        item {
            Column {
                Eyebrow("Conversations", color = c.accent)
                Spacer(Modifier.height(Space.sm))
                Text("Chats", style = MaterialTheme.typography.displayMedium, color = c.onSurface)
                Spacer(Modifier.height(Space.xs))
                Text(
                    "Only this trip. Nothing is kept after you stop sharing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
                Spacer(Modifier.height(Space.md))
            }
        }

        if (rows.isEmpty()) {
            item {
                EmptyState(
                    glyph = "💬",
                    title = "No conversations yet",
                    body = "Say hello to a traveller going your way and the chat will show up here."
                )
            }
        }

        items(rows, key = { it.key }) { row ->
            WayHomeCard(
                Modifier.fillMaxWidth(),
                onClick = {
                    if (row.kind == ConversationRow.Kind.Direct) {
                        onOpenDirect(row.endpointId, row.tempId)
                    } else {
                        onOpenGroup(row.tempId)
                    }
                }
            ) {
                Row(
                    Modifier.padding(Space.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (row.kind == ConversationRow.Kind.Direct) {
                        TravellerAvatar(row.tempId, size = 44.dp, ring = AvatarRing.Connected)
                    } else {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.accentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👥", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.width(Space.lg))
                    Column(Modifier.weight(1f)) {
                        Text(
                            row.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = c.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            row.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (row.kind == ConversationRow.Kind.Direct) {
                        StatusIndicator("", StatusTone.Good)
                    }
                }
            }
        }
    }
}
