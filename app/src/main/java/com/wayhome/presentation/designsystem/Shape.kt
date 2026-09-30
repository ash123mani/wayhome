package com.wayhome.presentation.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Generous, consistent radii. Cards are big and soft so the layout feels like
 * paper stock rather than a stack of grey rectangles.
 */
val WayHomeShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

/** The house card radius — used by nearly every surface in the app. */
val CardShape = RoundedCornerShape(24.dp)

/** The house control radius — buttons, chips, fields. */
val ControlShape = RoundedCornerShape(16.dp)

/** The house pill radius — chips, badges, the bottom bar. */
val PillShape = RoundedCornerShape(100.dp)

/** Bubbles carry one tightened corner instead of a drawn tail. */
val BubbleShapeMine = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp)
val BubbleShapeTheirs = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 6.dp, bottomStart = 20.dp)

object Space {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val section: Dp = 32.dp
    val gutter: Dp = 20.dp
}
