package com.wayhome.presentation.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * WayHome palette — warm bone neutrals and deep charcoal, carried by a
 * terracotta "go" accent with a dusk violet and sea teal for travel states.
 *
 * Colour is reserved for actions, status, journeys and selected states;
 * everything else stays neutral so one accent always means one thing.
 */
@Immutable
data class WayHomeColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceSunken: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineStrong: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val onAccentSoft: Color,
    val go: Color,
    val goSoft: Color,
    val hold: Color,
    val holdSoft: Color,
    val quiet: Color,
    val scrim: Color,
    /** Dusk violet — night rides, "boarding", secondary highlights. */
    val dusk: Color,
    val duskSoft: Color,
    /** Sea teal — live/active signals that must not read as the CTA. */
    val sea: Color,
    val seaSoft: Color,
    /** Dawn sky wash behind hero content, melting into [background]. */
    val skyTop: Color,
    val skyBottom: Color,
    /** Kraft stock for boarding-pass surfaces. */
    val kraft: Color,
    /** Perforation dashes on a ticket. */
    val perforation: Color
)

val WayHomeLightColors = WayHomeColors(
    background = Color(0xFFFAF6F1),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF3EDE6),
    surfaceSunken = Color(0xFFEDE5DC),
    onSurface = Color(0xFF1B1613),
    onSurfaceVariant = Color(0xFF6B5F57),
    outline = Color(0xFFE7DDD3),
    outlineStrong = Color(0xFFD2C4B7),
    accent = Color(0xFFC0552B),
    accentSoft = Color(0xFFF8E5D9),
    onAccent = Color(0xFFFFFFFF),
    onAccentSoft = Color(0xFF5C2610),
    go = Color(0xFF2E7D5B),
    goSoft = Color(0xFFDDEDE4),
    hold = Color(0xFFA9761B),
    holdSoft = Color(0xFFF7EBD6),
    quiet = Color(0xFF8A7D74),
    scrim = Color(0xFF1B1613),
    dusk = Color(0xFF5B4BC4),
    duskSoft = Color(0xFFEAE6FB),
    sea = Color(0xFF0E7C86),
    seaSoft = Color(0xFFDCEFF1),
    skyTop = Color(0xFFFFF2E6),
    skyBottom = Color(0xFFFAF6F1),
    kraft = Color(0xFFF7F0E5),
    perforation = Color(0xFFD2C4B7)
)

val WayHomeDarkColors = WayHomeColors(
    background = Color(0xFF14110F),
    surface = Color(0xFF1C1917),
    surfaceMuted = Color(0xFF262220),
    surfaceSunken = Color(0xFF100E0D),
    onSurface = Color(0xFFF6F1EC),
    onSurfaceVariant = Color(0xFFA99C93),
    outline = Color(0xFF322C29),
    outlineStrong = Color(0xFF4A423E),
    accent = Color(0xFFF0A57C),
    accentSoft = Color(0xFF45220F),
    onAccent = Color(0xFF3A1A0C),
    onAccentSoft = Color(0xFFF8D3BB),
    go = Color(0xFF74D3A0),
    goSoft = Color(0xFF16301F),
    hold = Color(0xFFE2B662),
    holdSoft = Color(0xFF312512),
    quiet = Color(0xFF7D716A),
    scrim = Color(0xFF000000),
    dusk = Color(0xFFB3A6FF),
    duskSoft = Color(0xFF241F45),
    sea = Color(0xFF6FD3DD),
    seaSoft = Color(0xFF0E2E33),
    skyTop = Color(0xFF251C16),
    skyBottom = Color(0xFF14110F),
    kraft = Color(0xFF221D19),
    perforation = Color(0xFF4A423E)
)

val LocalWayHomeColors = staticCompositionLocalOf { WayHomeLightColors }

/** Deterministic warm gradient for a temporary identity (no photos, no PII). */
fun avatarColors(tempId: String, dark: Boolean): Pair<Color, Color> {
    val h = tempId.hashCode()
    val hue = ((h % 41) + 6).toFloat()               // 6°..46° — sunset band
    val top = Color.hsv(hue, if (dark) 0.38f else 0.46f, if (dark) 0.72f else 0.98f)
    val bottom = Color.hsv(hue + 12f, if (dark) 0.55f else 0.62f, if (dark) 0.95f else 0.88f)
    return top to bottom
}
