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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayhome.presentation.designsystem.CardShape
import com.wayhome.presentation.designsystem.PillShape
import com.wayhome.presentation.designsystem.Space
import com.wayhome.presentation.designsystem.WayHome
import com.wayhome.presentation.designsystem.WayHomeTravelType
import com.wayhome.presentation.designsystem.aurora
import com.wayhome.presentation.designsystem.auroraWash

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
 * Aurora wash behind top-of-screen content. Melts into the background so it
 * never reads as a coloured band.
 */
@Composable
fun SkyWash(modifier: Modifier = Modifier, height: Dp = 260.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(WayHome.colors.auroraWash())
    )
}

/** Full-bleed backdrop for a screen: aurora wash over the flat background. */
@Composable
fun TravelBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = WayHome.colors
    Box(modifier.fillMaxSize().background(c.background)) {
        SkyWash(Modifier.align(Alignment.TopCenter))
        Box(Modifier.fillMaxSize()) { content() }
    }
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
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f), 0f)
        )
    }
}

/* --------------------------------------------------------------- the journey */

/**
 * The wordmark: an aurora pip plus the name in tracked caps. Used once, on
 * Welcome — anywhere else the brand is carried by the aurora itself.
 */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    val c = WayHome.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(c.aurora())
        )
        Spacer(Modifier.width(Space.sm))
        Text(
            "WAYHOME",
            style = WayHomeTravelType.Eyebrow,
            color = c.onSurface
        )
    }
}

/**
 * The app's anchor object: where you're going, how far, and whether anyone is
 * on the road with you. Reads like a boarding pass — origin, a dashed road, a
 * destination ring, then the perforated stub with the numbers.
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
    val shape = CardShape
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
            .border(
                1.dp,
                Brush.verticalGradient(listOf(c.hairline, c.outline)),
                shape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = label }
    ) {
        Column(
            Modifier.padding(
                start = Space.xl, end = Space.xl,
                top = Space.xl, bottom = Space.lg
            )
        ) {
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
            Spacer(Modifier.height(Space.sm))
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
                StubStat(
                    value = caption,
                    label = "Status",
                    tint = c.sea,
                    modifier = Modifier.weight(1f)
                )
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

/** Horizontal road: filled origin dot, dashed run, hollow destination ring. */
@Composable
private fun RouteDash(color: Color, dotColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.width(28.dp).height(12.dp)) {
        val y = size.height / 2
        drawCircle(dotColor, radius = 3.4f, center = Offset(0f, y))
        drawLine(
            color = color.copy(alpha = 0.5f),
            start = Offset(5.5f, y),
            end = Offset(size.width - 5.5f, y),
            strokeWidth = 1.7f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.5f, 3.5f), 0f)
        )
        drawCircle(dotColor, radius = 4.6f, center = Offset(size.width, y), style = Stroke(width = 2f))
    }
}

/**
 * The vertical journey rail — the app's signature motif. A filled origin dot,
 * a dashed road, then a ringed destination. Use it to show a trip as a
 * departure board column rather than as a form field.
 */
@Composable
fun JourneyRail(
    from: String,
    to: String,
    modifier: Modifier = Modifier,
    active: Boolean = true
) {
    val c = WayHome.colors
    val road = if (active) c.accent else c.outlineStrong

    Row(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .width(16.dp)
                .height(56.dp)
        ) {
            val x = size.width / 2f
            drawCircle(road, radius = 4.5f, center = Offset(x, 5f))
            drawLine(
                color = road.copy(alpha = if (active) 0.55f else 0.35f),
                start = Offset(x, 11f),
                end = Offset(x, size.height - 6f),
                strokeWidth = 1.8f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )
            drawCircle(road, radius = 6.5f, center = Offset(x, size.height - 5f), style = Stroke(width = 2.2f))
        }
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Text(
                from.uppercase().ifBlank { "Not set" },
                style = WayHomeTravelType.StubLabel,
                color = c.quiet
            )
            Spacer(Modifier.height(Space.xxs))
            Text(
                to.ifBlank { "Choose a destination" },
                style = MaterialTheme.typography.titleMedium,
                color = if (to.isBlank()) c.quiet else c.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ------------------------------------------------------------ status surfaces */

/**
 * Departure-board row: a live dot, a mono status, optional trailing value.
 * Replaces the old scatter of pill badges.
 */
@Composable
fun DepartureRow(
    label: String,
    value: String? = null,
    tone: StatusTone = StatusTone.Good,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    val c = WayHome.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Space.md))
        } else {
            StatusDot(tone)
            Spacer(Modifier.width(Space.md))
        }
        Text(
            label,
            style = WayHomeTravelType.Ticket,
            color = c.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Spacer(Modifier.width(Space.sm))
            Text(
                value,
                style = WayHomeTravelType.Ticket,
                color = c.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** The live signal dot on its own — gains a soft halo when [pulsing]. */
@Composable
fun LiveDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    pulsing: Boolean = false
) {
    Box(
        modifier
            .size(if (pulsing) size * 2.2f else size)
            .clip(CircleShape)
            .background(if (pulsing) color.copy(alpha = 0.18f) else color),
        contentAlignment = Alignment.Center
    ) {
        if (pulsing) {
            Box(Modifier.size(size).clip(CircleShape).background(color))
        }
    }
}

/** Same dot, driven by a semantic tone. */
@Composable
fun StatusDot(
    tone: StatusTone,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    pulsing: Boolean = false
) {
    val c = WayHome.colors
    LiveDot(
        color = when (tone) {
            StatusTone.Good -> c.go
            StatusTone.Pending -> c.hold
            StatusTone.Quiet -> c.quiet
        },
        modifier = modifier,
        size = size,
        pulsing = pulsing
    )
}

/** Compact pill for a single fact — "Going your way", "Free to join". */
@Composable
fun StatusPill(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier
) {
    val c = WayHome.colors
    val (bg, fg) = when (tone) {
        StatusTone.Good -> c.goSoft to c.go
        StatusTone.Pending -> c.holdSoft to c.hold
        StatusTone.Quiet -> c.surfaceMuted to c.onSurfaceVariant
    }
    Row(
        modifier
            .clip(PillShape)
            .background(bg)
            .padding(horizontal = Space.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(fg))
        Spacer(Modifier.width(Space.sm))
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

/**
 * A card lit by the aurora: a soft vertical wash plus a gradient hairline, used
 * for the one surface per screen that deserves the brand glow.
 */
@Composable
fun AuroraCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val c = WayHome.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(c.accent.copy(alpha = 0.14f), c.surface, c.surface)
                )
            )
            .border(1.dp, c.aurora(alpha = 0.34f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) { content() }
}

/**
 * The shared tab header. Every top-level screen uses this so the four tabs feel
 * like one product instead of four designs.
 */
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val c = WayHome.colors
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(eyebrow, Modifier.weight(1f), color = c.accent)
            trailing?.invoke()
        }
        Spacer(Modifier.height(Space.sm))
        Text(
            title,
            style = MaterialTheme.typography.displaySmall,
            color = c.onSurface
        )
        if (subtitle != null) {
            Spacer(Modifier.height(Space.xs))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant
            )
        }
    }
}

/** Big number + label, for the "3 going your way" moments. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = WayHome.colors.onSurface
) {
    val c = WayHome.colors
    Column(modifier) {
        Text(
            value,
            style = WayHomeTravelType.TicketLarge,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(Space.xxs))
        Text(label.uppercase(), style = WayHomeTravelType.StubLabel, color = c.quiet)
    }
}

private fun formatKm(km: Double): String =
    if (km >= 10) "${km.toInt()} km" else String.format("%.1f km", km)
