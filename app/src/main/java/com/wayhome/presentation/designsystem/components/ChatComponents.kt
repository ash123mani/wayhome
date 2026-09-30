package com.wayhome.presentation.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wayhome.presentation.designsystem.BubbleShapeMine
import com.wayhome.presentation.designsystem.BubbleShapeTheirs
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val clock = SimpleDateFormat("HH:mm", Locale.getDefault())

/** Temporary coordination chat — deliberately not a messenger. */
@Composable
fun MessageBubble(
    text: String,
    timestamp: Long,
    mine: Boolean,
    modifier: Modifier = Modifier,
    senderName: String? = null,
    sending: Boolean = false
) {
    val c = WayHome.colors
    val bg = if (mine) c.accent else c.surfaceMuted
    val fg = if (mine) c.onAccent else c.onSurface
    val nameFg = if (mine) c.onAccent else c.accent

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
    ) {
        if (!mine && senderName != null) {
            Text(
                senderName,
                style = MaterialTheme.typography.labelMedium,
                color = nameFg,
                modifier = Modifier.padding(start = Space.md, bottom = Space.xs)
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                Modifier
                    .clip(if (mine) BubbleShapeMine else BubbleShapeTheirs)
                    .background(bg)
                    .padding(horizontal = Space.lg, vertical = Space.md)
            ) {
                Text(text, style = MaterialTheme.typography.bodyLarge, color = fg)
            }
        }
        Row(
            Modifier.padding(horizontal = Space.sm, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(clock.format(Date(timestamp)), style = MaterialTheme.typography.labelSmall, color = c.quiet)
            if (mine) {
                Spacer(Modifier.width(Space.xs))
                Text(
                    if (sending) "sending" else "sent",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.quiet
                )
            }
        }
    }
}

/** Tappable openers so nobody has to think of a first message. */
@Composable
fun QuickReplies(
    suggestions: List<String>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (suggestions.isEmpty()) return
    val c = WayHome.colors
    val haptics = LocalHapticFeedback.current
    Column(modifier.fillMaxWidth()) {
        Text(
            "Quick messages",
            style = MaterialTheme.typography.labelMedium,
            color = c.quiet,
            modifier = Modifier.padding(start = Space.xs, bottom = Space.sm)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            items(suggestions) { s ->
                Box(
                    Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.surface)
                        .border(BorderStroke(1.dp, c.outline), RoundedCornerShape(20.dp))
                        .clickable(enabled = enabled) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPick(s)
                        }
                        .padding(horizontal = Space.lg, vertical = Space.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        s,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Type a message…"
) {
    val c = WayHome.colors
    val enabled = value.isNotBlank()
    val scale by animateFloatAsState(
        if (enabled) 1f else 0.9f,
        spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
        label = "sendScale"
    )
    val haptics = LocalHapticFeedback.current

    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .border(BorderStroke(1.dp, c.outline), RoundedCornerShape(20.dp))
                .padding(horizontal = Space.lg, vertical = Space.md)
        ) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.quiet)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyLarge
                ).copy(color = c.onSurface),
                cursorBrush = SolidColor(c.accent),
                maxLines = 4,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = { if (enabled) onSend() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Message field" }
            )
        }
        Spacer(Modifier.width(Space.sm))
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (enabled) c.accent else c.surfaceMuted)
                .clickable(enabled = enabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSend()
                }
                .semantics { contentDescription = "Send message" },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (enabled) Icons.Filled.ArrowUpward else Icons.Outlined.Add,
                contentDescription = null,
                tint = if (enabled) c.onAccent else c.quiet,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Small centred system line: "Traveller 923 joined", "Group created". */
@Composable
fun SystemNote(text: String, modifier: Modifier = Modifier) {
    val c = WayHome.colors
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = Space.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = c.quiet,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(c.surfaceMuted)
                .padding(horizontal = Space.md, vertical = Space.xs)
        )
    }
}

@Composable
fun TypingIndicator(active: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = active, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Text("…", style = MaterialTheme.typography.bodyMedium, color = WayHome.colors.quiet)
    }
}
