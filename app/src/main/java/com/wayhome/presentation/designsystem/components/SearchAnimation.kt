package com.wayhome.presentation.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayhome.presentation.designsystem.WayHome
import kotlinx.coroutines.delay

/**
 * Contextual "actively searching" motion — expanding rings plus one dot per
 * traveller found. Deliberately not a spinner: the ring count grows as people
 * appear, so the screen tells the user the world is being scanned.
 */
@Composable
fun SearchAnimation(
    found: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 220.dp,
    active: Boolean = true
) {
    val c = WayHome.colors
    val transition = rememberInfiniteTransition(label = "search")
    val spin = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val rings = listOf(0, 1, 2).map { i ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, delayMillis = i * 850, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring$i"
        ).value
    }
    val density = LocalDensity.current
    val dotCount = found.coerceIn(0, 6)

    Box(
        modifier
            .size(diameter)
            .semantics {
                contentDescription =
                    if (found > 0) "Searching nearby, $found travellers found"
                    else "Searching for nearby travellers"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(diameter)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            if (active) {
                rings.forEach { p ->
                    val r = radius * (0.32f + 0.66f * p)
                    drawCircle(
                        color = c.accent.copy(alpha = (1f - p) * 0.42f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // one orbiting dot per traveller discovered
            if (dotCount > 0) {
                val orbit = radius * 0.68f
                val dot = with(density) { 5.dp.toPx() }
                repeat(dotCount) { i ->
                    val angleRad = Math.toRadians((spin.value + i * (360.0 / dotCount)).toDouble())
                    val dx = center.x + (orbit * kotlin.math.cos(angleRad)).toFloat()
                    val dy = center.y + (orbit * kotlin.math.sin(angleRad)).toFloat()
                    drawCircle(c.accent, dot, Offset(dx, dy))
                    drawCircle(c.accent.copy(alpha = 0.18f), dot * 2.4f, Offset(dx, dy))
                }
            }

            // heart of the search
            drawCircle(c.accent.copy(alpha = 0.10f), radius * 0.26f, center)
            drawCircle(c.accent, radius * 0.085f, center)
        }
    }
}

/** Fade + lift used for peers appearing and rows entering lists. */
@Composable
fun Modifier.appearIn(delayMillis: Int = 0): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        progress.animateTo(1f, tween(durationMillis = 360, easing = FastOutSlowInEasing))
    }
    val p = progress.value
    return this.graphicsLayer {
        alpha = p
        translationY = (1f - p) * 16.dp.toPx()
        val s = 0.98f + 0.02f * p
        scaleX = s
        scaleY = s
    }
}
