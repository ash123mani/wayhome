package com.wayhome.presentation.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * WayHome palette — "Departure Board".
 *
 * The app is a boarding pass to home, so the canvas is a terminal at night:
 * deep indigo-plum with a light "departure paper" mode beside it. Colour is
 * rationed so one accent always means one thing.
 *
 *  - [accent] iris      → the single primary action. Nothing else.
 *  - [sea]   coral     → live / active / right now (never a CTA).
 *  - [dusk]  violet    → route + journey highlights, the aurora gradient.
 *  - [go]    mint      → verified, connected, safe.
 *  - [hold]  amber     → pending, offline, needs attention.
 *
 * Every surface also carries a `hairline` (a lighter top edge) so cards read as
 * lit from above rather than as flat stickers.
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
    /** Violet — journey/route highlights, and the middle of the aurora ramp. */
    val dusk: Color,
    val duskSoft: Color,
    /** Coral — live signals that must not read as the CTA. */
    val sea: Color,
    val seaSoft: Color,
    /** Aurora hero wash: melts from [skyTop] into [skyBottom]. */
    val skyTop: Color,
    val skyBottom: Color,
    /** Ticket/stub stock. */
    val kraft: Color,
    /** Perforation dashes. */
    val perforation: Color,
    /** Top-edge highlight on raised surfaces. */
    val hairline: Color,
    /** Ambient colour for glows, radar sweeps and the primary button ramp. */
    val glow: Color,
    /** Text/graphic colour that sits *on* an aurora or accent fill. */
    val onGlow: Color
)

val WayHomeLightColors = WayHomeColors(
    background = Color(0xFFF5F4F9),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEEEDF5),
    surfaceSunken = Color(0xFFE6E5EF),
    onSurface = Color(0xFF13122A),
    onSurfaceVariant = Color(0xFF5A5972),
    outline = Color(0xFFE2E1EC),
    outlineStrong = Color(0xFFC9C7DA),
    accent = Color(0xFF5B4BE8),
    accentSoft = Color(0xFFE8E5FD),
    onAccent = Color(0xFFFFFFFF),
    onAccentSoft = Color(0xFF241C63),
    go = Color(0xFF0C8F63),
    goSoft = Color(0xFFD8F3E7),
    hold = Color(0xFF9C5B06),
    holdSoft = Color(0xFFFBEBD2),
    quiet = Color(0xFF8A89A3),
    scrim = Color(0xFF13122A),
    dusk = Color(0xFF7B4BE8),
    duskSoft = Color(0xFFEFE8FE),
    sea = Color(0xFFE14E2C),
    seaSoft = Color(0xFFFFE7E0),
    skyTop = Color(0xFFE9E4FF),
    skyBottom = Color(0xFFF5F4F9),
    kraft = Color(0xFFF3F1FA),
    perforation = Color(0xFFC9C7DA),
    hairline = Color(0xFFFFFFFF),
    glow = Color(0xFF8B7BFF),
    onGlow = Color(0xFFFFFFFF)
)

val WayHomeDarkColors = WayHomeColors(
    background = Color(0xFF0A0A14),
    surface = Color(0xFF14141F),
    surfaceMuted = Color(0xFF1D1D2B),
    surfaceSunken = Color(0xFF08080E),
    onSurface = Color(0xFFF4F3FA),
    onSurfaceVariant = Color(0xFFA8A7BF),
    outline = Color(0xFF262638),
    outlineStrong = Color(0xFF3A3A55),
    accent = Color(0xFF8B7CFF),
    accentSoft = Color(0xFF231F4A),
    onAccent = Color(0xFF100C2B),
    onAccentSoft = Color(0xFFD7D1FF),
    go = Color(0xFF4ADFA0),
    goSoft = Color(0xFF0C2F22),
    hold = Color(0xFFF7C36F),
    holdSoft = Color(0xFF35280C),
    quiet = Color(0xFF7A7994),
    scrim = Color(0xFF000000),
    dusk = Color(0xFFB69BFF),
    duskSoft = Color(0xFF251E4A),
    sea = Color(0xFFFF8565),
    seaSoft = Color(0xFF3A1810),
    skyTop = Color(0xFF1B1740),
    skyBottom = Color(0xFF0A0A14),
    kraft = Color(0xFF1A1A27),
    perforation = Color(0xFF3A3A55),
    hairline = Color(0xFF2A2A3E),
    glow = Color(0xFF8B7CFF),
    onGlow = Color(0xFF0B0820)
)

val LocalWayHomeColors = staticCompositionLocalOf { WayHomeLightColors }

/**
 * The signature ramp: iris → violet → coral. One aurora for the whole app, used
 * by the hero wash, the primary button and the search radar so those three
 * always read as the same brand surface. Pass [alpha] for a tinted version.
 */
fun WayHomeColors.aurora(alpha: Float = 1f): Brush = Brush.linearGradient(
    if (alpha >= 1f) listOf(accent, dusk, sea)
    else listOf(accent.copy(alpha = alpha), dusk.copy(alpha = alpha), sea.copy(alpha = alpha))
)

/** Softer vertical variant for full-bleed screen backdrops. */
fun WayHomeColors.auroraWash(): Brush = Brush.verticalGradient(listOf(skyTop, skyBottom))

/** Very low-alpha aurora for cards sitting on the aurora wash. */
fun WayHomeColors.auroraVeil(): Brush = Brush.linearGradient(
    listOf(accent.copy(alpha = 0.16f), dusk.copy(alpha = 0.10f), sea.copy(alpha = 0.14f))
)

/** Deterministic gradient for a temporary identity — no photos, no PII. */
fun avatarColors(tempId: String, dark: Boolean): Pair<Color, Color> {
    val h = tempId.hashCode()
    val hue = ((h % 360) + 360) % 360
    val top = Color.hsv(hue.toFloat(), if (dark) 0.44f else 0.52f, if (dark) 0.74f else 0.96f)
    val bottom = Color.hsv(
        ((hue + 38) % 360).toFloat(),
        if (dark) 0.62f else 0.70f,
        if (dark) 0.98f else 0.84f
    )
    return top to bottom
}
