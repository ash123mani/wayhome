package com.wayhome.presentation.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * WayHome reads like a departure board, not a settings screen.
 *
 * Three system families carry the whole voice — no font files, no network:
 *  - [Serif]  → editorial display. The "printed ticket" voice.
 *  - [Sans]   → UI, body copy, controls.
 *  - [Mono]   → travel data: distances, times, IDs, statuses.
 *
 * Display sizes are deliberately large and tightly tracked (−1.2sp at 40sp) so
 * headlines read as one confident block instead of loose Material defaults.
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
    displayLarge = editorial(52, 55, -1.9),
    displayMedium = editorial(40, 43, -1.3),
    displaySmall = editorial(32, 36, -0.9),

    headlineLarge = editorial(27, 32, -0.6),
    headlineMedium = editorial(22, 27, -0.4),
    headlineSmall = editorial(18, 23, -0.2),

    titleLarge = ui(18.0, 24, -0.2, FontWeight.SemiBold),
    titleMedium = ui(15.5, 21, -0.1, FontWeight.SemiBold),
    titleSmall = ui(14.0, 19, 0.0, FontWeight.Medium),

    bodyLarge = ui(16.0, 24, 0.0, FontWeight.Normal),
    bodyMedium = ui(14.5, 21, 0.0, FontWeight.Normal),
    bodySmall = ui(13.0, 18, 0.0, FontWeight.Normal),

    labelLarge = ui(14.0, 18, 0.0, FontWeight.SemiBold),
    labelMedium = ui(12.0, 15, 0.1, FontWeight.Medium),
    labelSmall = ui(11.0, 14, 0.1, FontWeight.Medium)
)

/**
 * Roles Material's [Typography] has no slot for. Travel UI lives on these.
 */
object WayHomeTravelType {

    /** Uppercase kicker above a headline — "TONIGHT · 21:40". */
    val Eyebrow = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.6.sp,
        platformStyle = Trim
    )

    /** Departure-board data: distances, times, counters, route numbers. */
    val Ticket = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        platformStyle = Trim
    )

    /** Oversized mono numeral for a headline metric ("2.4 km"). */
    val TicketLarge = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.8).sp,
        platformStyle = Trim
    )

    /** Hero display for the brand moment — one line, as large as it fits. */
    val Hero = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 41.sp,
        letterSpacing = (-1.4).sp,
        platformStyle = Trim,
        lineHeightStyle = TightLineHeight
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
        letterSpacing = 1.1.sp,
        platformStyle = Trim
    )
}
