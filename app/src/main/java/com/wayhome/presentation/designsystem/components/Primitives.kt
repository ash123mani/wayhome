package com.wayhome.presentation.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayhome.domain.model.MatchLevel
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.avatarColors

/* ------------------------------------------------------------------ buttons */

enum class WayHomeButtonStyle { Primary, Tonal, Outline, Ghost }

@Composable
fun WayHomeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: WayHomeButtonStyle = WayHomeButtonStyle.Primary,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    haptic: Boolean = true
) {
    val c = WayHome.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press"
    )
    val haptics = LocalHapticFeedback.current

    val bg = when (style) {
        WayHomeButtonStyle.Primary -> c.accent
        WayHomeButtonStyle.Tonal -> c.accentSoft
        WayHomeButtonStyle.Outline -> Color.Transparent
        WayHomeButtonStyle.Ghost -> Color.Transparent
    }
    val fg = when (style) {
        WayHomeButtonStyle.Primary -> c.onAccent
        WayHomeButtonStyle.Tonal -> c.onAccentSoft
        WayHomeButtonStyle.Outline -> c.onSurface
        WayHomeButtonStyle.Ghost -> c.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .heightIn(min = 52.dp)                       // generous touch target
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) bg else c.surfaceMuted)
            .then(
                if (style == WayHomeButtonStyle.Outline)
                    Modifier.border(BorderStroke(1.dp, c.outlineStrong), RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled
            ) {
                if (haptic) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = Space.xl, vertical = Space.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = if (enabled) fg else c.quiet,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Space.sm))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) fg else c.quiet
        )
    }
}

/* -------------------------------------------------------------------- cards */

@Composable
fun WayHomeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(22.dp),
    container: Color = WayHome.colors.surface,
    border: Color = WayHome.colors.outline,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.99f else 1f,
        spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "cardPress"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(container)
            .border(BorderStroke(1.dp, border), shape)
            .then(if (onClick != null) Modifier.clickable(
                interactionSource = interaction, indication = null, onClick = onClick
            ) else Modifier)
    ) { content() }
}

/* ------------------------------------------------------------------ avatars */

enum class AvatarRing { None, Searching, Connected, Muted }

@Composable
fun TravellerAvatar(
    tempId: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    ring: AvatarRing = AvatarRing.None,
    dimmed: Boolean = false
) {
    val c = WayHome.colors
    val (top, bottom) = avatarColors(tempId, WayHome.isDark)
    val initials = remember(tempId) {
        val digits = tempId.filter { it.isDigit() }
        if (digits.isNotEmpty()) digits.takeLast(2) else tempId.take(1).uppercase()
    }
    val ringColor by animateColorAsState(
        when (ring) {
            AvatarRing.Connected -> c.go
            AvatarRing.Searching -> c.accent
            AvatarRing.Muted -> c.outlineStrong
            AvatarRing.None -> Color.Transparent
        },
        tween(400),
        label = "ring"
    )
    val breathe by animateFloatAsState(
        if (ring == AvatarRing.Searching) 1f else 0f,
        tween(600),
        label = "breathe"
    )

    Box(
        modifier = modifier
            .size(size + if (ring == AvatarRing.None) 0.dp else 6.dp)
            .semantics {
                contentDescription = "$tempId, temporary traveller avatar"
            },
        contentAlignment = Alignment.Center
    ) {
        if (ring != AvatarRing.None) {
            Box(
                Modifier
                    .size(size + 6.dp)
                    .alpha(0.55f + 0.45f * breathe)
                    .clip(CircleShape)
                    .border(1.5.dp, ringColor, CircleShape)
            )
        }
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(top, bottom)))
                .alpha(if (dimmed) 0.5f else 1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials,
                style = MaterialTheme.typography.titleMedium,
                color = if (WayHome.isDark) Color(0xFF2A1710) else Color.White
            )
        }
    }
}

/** Overlapping avatars for "N people nearby" clusters. */
@Composable
fun AvatarStack(
    tempIds: List<String>,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    max: Int = 4
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-size * 0.28f))) {
        tempIds.take(max).forEach { id ->
            TravellerAvatar(tempId = id, size = size)
        }
    }
}

/* --------------------------------------------------------------------- misc */

@Composable
fun StatusIndicator(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier
) {
    val c = WayHome.colors
    val color by animateColorAsState(
        when (tone) {
            StatusTone.Good -> c.go
            StatusTone.Pending -> c.hold
            StatusTone.Quiet -> c.quiet
        },
        label = "statusTone"
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(Space.sm))
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
    }
}

enum class StatusTone { Good, Pending, Quiet }

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = WayHome.colors.onSurface)
        trailing?.invoke()
    }
}

@Composable
fun DestinationChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = WayHome.colors
    val haptics = LocalHapticFeedback.current
    val bg by animateColorAsState(if (selected) c.accent else c.surface, tween(220), label = "chipBg")
    val fg by animateColorAsState(if (selected) c.onAccent else c.onSurface, tween(220), label = "chipFg")
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                BorderStroke(1.dp, if (selected) Color.Transparent else c.outline),
                RoundedCornerShape(14.dp)
            )
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = Space.lg, vertical = Space.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EmptyState(
    glyph: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val c = WayHome.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = Space.section),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(c.surfaceMuted),
            contentAlignment = Alignment.Center
        ) { Text(glyph, style = MaterialTheme.typography.headlineMedium) }
        Spacer(Modifier.height(Space.lg))
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.onSurface)
        Spacer(Modifier.height(Space.sm))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.xl)
        )
        if (action != null) {
            Spacer(Modifier.height(Space.xl))
            action()
        }
    }
}

/** Reassuring, non-technical connectivity copy. Never says BLE/mesh. */
@Composable
fun OfflineBanner(
    internetAvailable: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val c = WayHome.colors
    val (title, subtitle, tone) = if (internetAvailable) {
        Triple("Nearby mode active", "Finding travellers around you right now", c.sea)
    } else {
        Triple("Offline nearby mode", "Still finds people nearby — no internet needed", c.hold)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(BorderStroke(1.dp, c.outline), RoundedCornerShape(16.dp))
            .padding(horizontal = Space.lg, vertical = if (compact) Space.md else Space.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(tone))
        Spacer(Modifier.width(Space.md))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.onSurface)
            if (!compact) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
    }
}

/* ------------------------------------------------------------------ matching */

data class MatchCopy(val label: String, val tone: StatusTone)

@Composable
fun matchCopy(level: MatchLevel): MatchCopy = when (level) {
    MatchLevel.SAME_AREA -> MatchCopy("Same destination", StatusTone.Good)
    MatchLevel.NEARBY_AREA -> MatchCopy("Nearby destination", StatusTone.Pending)
    MatchLevel.SAME_CITY -> MatchCopy("Same city", StatusTone.Quiet)
    MatchLevel.DIFFERENT -> MatchCopy("Nearby, different way", StatusTone.Quiet)
}

@Composable
fun MatchBadge(level: MatchLevel, modifier: Modifier = Modifier) {
    val c = WayHome.colors
    val copy = matchCopy(level)
    val (bg, fg) = when (copy.tone) {
        StatusTone.Good -> c.goSoft to c.go
        StatusTone.Pending -> c.holdSoft to c.hold
        StatusTone.Quiet -> c.surfaceMuted to c.onSurfaceVariant
    }
    Box(
        modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .padding(horizontal = Space.sm, vertical = 5.dp)
    ) {
        Text(copy.label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

/** "You → Whitefield ⇄ Traveller 281 → Marathahalli" made human. */
@Composable
fun MatchPath(
    myArea: String,
    theirArea: String,
    level: MatchLevel,
    modifier: Modifier = Modifier
) {
    val c = WayHome.colors
    val copy = matchCopy(level)
    val connector by animateFloatAsState(
        if (level == MatchLevel.SAME_AREA) 1f else 0.6f,
        tween(500),
        label = "connector"
    )
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("You're going", style = MaterialTheme.typography.labelMedium, color = c.quiet)
            Text(
                myArea.ifBlank { "Your area" },
                style = MaterialTheme.typography.titleSmall,
                color = c.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(Space.md))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .width(52.dp)
                    .height(1.dp)
                    .background(c.outlineStrong)
            )
            Spacer(Modifier.height(Space.xs))
            Text("▼", style = MaterialTheme.typography.labelSmall, color = c.outlineStrong)
        }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text("They're going", style = MaterialTheme.typography.labelMedium, color = c.quiet)
            Text(
                theirArea.ifBlank { "Another area" },
                style = MaterialTheme.typography.titleSmall,
                color = c.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Spacer(Modifier.height(Space.sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .alpha(connector)
                .background(if (level == MatchLevel.SAME_AREA) c.go else c.hold)
        )
        Spacer(Modifier.width(Space.sm))
        Text(
            copy.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (level == MatchLevel.SAME_AREA) c.go else c.onSurfaceVariant
        )
    }
}
