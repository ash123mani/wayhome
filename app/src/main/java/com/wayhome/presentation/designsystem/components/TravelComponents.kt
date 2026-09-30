package com.wayhome.presentation.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.WayHomeTravelType

/** Uppercase kicker. Travel UI labels its sections instead of shouting. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = WayHome.colors.quiet
) {
    Text(
        text = text.uppercase(),
        style = WayHomeTravelType.Eyebrow,
        color = color,
        modifier = modifier
    )
}

/**
 * Dawn wash behind top-of-screen content. Melts into [com.wayhome.presentation
 * .designsystem.WayHomeColors.background] so it never reads as a coloured band.
 */
@Composable
fun SkyWash(modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 220.dp) {
    val c = WayHome.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(listOf(c.skyTop, c.skyBottom)))
    )
}

/** Torn ticket edge. */
@Composable
fun Perforation(modifier: Modifier = Modifier, color: Color = WayHome.colors.perforation) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    }
}

/**
 * The journey, told like a boarding pass: where you're going, how far, and
 * whether anyone is on the road with you. This is the app's anchor object —
 * it replaces a plain "city / area" row on Home and Profile.
 */
@Composable
fun RouteTicket(
    city: String,
    area: String,
    distanceKm: Double? = null,
    etaLabel: String? = null,
    caption: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val c = WayHome.colors
    val shape = RoundedCornerShape(24.dp)
    val label = buildString {
        append("Your journey to ")
        append(area.ifBlank { "your destination" })
        if (city.isNotBlank()) append(", $city")
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = label }
    ) {
        Column(Modifier.padding(start = Space.xl, end = Space.xl, top = Space.xl, bottom = Space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Your journey", Modifier.weight(1f))
                if (onClick != null) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(c.surfaceMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Change destination",
                            tint = c.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(Space.md))
            Text(
                city.ifBlank { "Where are you" },
                style = MaterialTheme.typography.headlineMedium,
                color = c.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(Space.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RouteDash(color = c.accent, dotColor = c.accent)
                Spacer(Modifier.width(Space.md))
                Text(
                    area.ifBlank { "Pick an area to head towards" },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (area.isBlank()) c.quiet else c.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Perforation()

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xl, vertical = Space.lg),
            horizontalArrangement = Arrangement.spacedBy(Space.xl)
        ) {
            if (distanceKm != null) {
                StubStat(value = formatKm(distanceKm), label = "To go", tint = c.onSurface)
            }
            if (etaLabel != null) {
                StubStat(value = etaLabel, label = "Getting there", tint = c.onSurface)
            }
            if (caption != null) {
                StubStat(value = caption, label = "Status", tint = c.sea, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StubStat(
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val c = WayHome.colors
    Column(modifier) {
        Text(
            value,
            style = WayHomeTravelType.Ticket,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(Space.xxs))
        Text(label.uppercase(), style = WayHomeTravelType.StubLabel, color = c.quiet)
    }
}

/** Dotted road between two stops, capped with a destination pin dot. */
@Composable
private fun RouteDash(color: Color, dotColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.width(26.dp).height(10.dp)) {
        val y = size.height / 2
        drawCircle(dotColor, radius = 3.2f, center = Offset(0f, y))
        drawLine(
            color = color.copy(alpha = 0.55f),
            start = Offset(5f, y),
            end = Offset(size.width - 5f, y),
            strokeWidth = 1.6f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.5f, 3.5f), 0f)
        )
        drawCircle(dotColor, radius = 4.4f, center = Offset(size.width, y), style = Stroke(width = 2f))
    }
}

private fun formatKm(km: Double): String =
    if (km >= 10) "${km.toInt()} km" else String.format("%.1f km", km)

/** Full-bleed backdrop for a screen: sky wash over the flat background. */
@Composable
fun TravelBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = WayHome.colors
    Box(modifier.fillMaxSize().background(c.background)) {
        SkyWash(Modifier.align(Alignment.TopCenter))
        Box(Modifier.fillMaxSize()) { content() }
    }
}
