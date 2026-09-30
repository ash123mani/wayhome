package com.wayhome.presentation.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.aurora

enum class WayHomeTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home),
    Chats("Chats", Icons.Outlined.ChatBubbleOutline),
    Groups("Groups", Icons.Outlined.Groups),
    You("You", Icons.Outlined.PersonOutline)
}

/**
 * A floating rail rather than a full-width bar: the selected tab sits on the
 * aurora ramp so "where am I" is answered by colour, not just by a label.
 */
@Composable
fun WayHomeBottomBar(
    selected: WayHomeTab,
    onSelect: (WayHomeTab) -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    val c = WayHome.colors
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier
            .fillMaxWidth()
            .background(c.background)
            .padding(horizontal = Space.lg)
            .padding(bottom = Space.md)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.surface)
                .border(1.dp, c.outline, shape)
                .navigationBarsPadding()
                .padding(horizontal = Space.xs, vertical = Space.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WayHomeTab.entries.forEach { tab ->
                BottomBarItem(
                    tab = tab,
                    selected = tab == selected,
                    badge = if (tab == WayHomeTab.Chats) badgeCount else 0,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(tab)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    tab: WayHomeTab,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = WayHome.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tabPress"
    )
    val tint by animateColorAsState(
        if (selected) c.onAccent else c.quiet,
        tween(200),
        label = "tabTint"
    )
    val content by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "tabContent")

    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = "${tab.label} tab${if (selected) ", selected" else ""}" }
            .padding(vertical = Space.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(width = 48.dp, height = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (selected) Modifier.background(c.aurora()) else Modifier
                    )
            )
            Icon(
                tab.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(20.dp)
                    .scale(scale)
            )
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(c.sea)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (content > 0.5f) c.onSurface else c.quiet
        )
    }
}
