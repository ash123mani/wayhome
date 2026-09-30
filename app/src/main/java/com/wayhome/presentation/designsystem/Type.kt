package com.wayhome.presentation.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * WayHome reads like a travel guide, not a settings screen.
 *
 * Three system families carry the whole voice — no font files, no network:
 *  - [Serif]  → editorial headlines (Noto Serif). The "printed guidebook" voice.
 *  - [Sans]   → UI, body copy, controls (Roboto).
 *  - [Mono]   → ticket data: gate numbers, ETAs, distances, times (monospace).
 *
 * Every style opts out of font padding and trims the first line's extra
 * leading, which is what makes default Material feel loose next to this.
 */
private val Sans = FontFamily.SansSerif
private val Serif = FontFamily.Serif
private val Mono = FontFamily.Monospace

private val Trim = PlatformTextStyle(includeFontPadding = false)

private val TightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun editorial(
    size: Int,
    line: Int,
    tracking: Double,
    weight: FontWeight = FontWeight.SemiBold
) = TextStyle(
    fontFamily = Serif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
    platformStyle = Trim,
    lineHeightStyle = TightLineHeight
)

private fun ui(
    size: Double,
    line: Int,
    tracking: Double,
    weight: FontWeight
) = TextStyle(
    fontFamily = Sans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
    platformStyle = Trim,
    lineHeightStyle = TightLineHeight
)

val WayHomeType = Typography(
    displayLarge = editorial(44, 48, -1.2),
    displayMedium = editorial(36, 40, -0.9),
    displaySmall = editorial(30, 35, -0.6),

    headlineLarge = editorial(27, 32, -0.5),
    headlineMedium = editorial(23, 28, -0.4),
    headlineSmall = editorial(20, 26, -0.2),

    titleLarge = ui(19.0, 25, -0.2, FontWeight.SemiBold),
    titleMedium = ui(16.0, 22, -0.1, FontWeight.Medium),
    titleSmall = ui(14.0, 19, 0.0, FontWeight.Medium),

    bodyLarge = ui(16.0, 24, 0.0, FontWeight.Normal),
    bodyMedium = ui(14.5, 21, 0.0, FontWeight.Normal),
    bodySmall = ui(13.0, 18, 0.0, FontWeight.Normal),

    labelLarge = ui(14.0, 18, 0.1, FontWeight.Medium),
    labelMedium = ui(12.0, 15, 0.2, FontWeight.Medium),
    labelSmall = ui(11.0, 14, 0.3, FontWeight.Medium)
)

/**
 * Roles Material's [Typography] has no slot for. Travel UI lives on these.
 */
object WayHomeTravelType {

    /** Uppercase kicker above a headline — "TONIGHT · 9:40 PM". */
    val Eyebrow = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp,
        platformStyle = Trim
    )

    /** Boarding-pass data: ETAs, distances, gate-style counters. */
    val Ticket = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        platformStyle = Trim
    )

    /** Oversized mono numeral for a headline metric ("2.4 km"). */
    val TicketLarge = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.5).sp,
        platformStyle = Trim
    )

    /** Editorial serif for a single line inside dense cards. */
    val Quote = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.1).sp,
        platformStyle = Trim
    )

    /** Small caps label on a boarding-pass stub. */
    val StubLabel = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 9.5.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.9.sp,
        platformStyle = Trim
    )
}

/** Convenience for `letterSpacing = 0.08.em` style tweaks at call sites. */
val Double.em: androidx.compose.ui.unit.TextUnit
    get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Em)
