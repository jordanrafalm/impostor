package com.impostor.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val SNOWFLAKE_COUNT = 24
private const val SNOW_CYCLE_MILLIS = 36_000

internal fun snowflakeProgress(cycle: Float, speed: Int, phase: Float): Float =
    (cycle * speed + phase) % 1f

/** Sparse decorative snowfall; every flake wraps outside the viewport on its own schedule. */
@Composable
internal fun SnowfallBackdrop(modifier: Modifier = Modifier) {
    val cycle = rememberInfiniteTransition(label = "snowfall").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(SNOW_CYCLE_MILLIS, easing = LinearEasing)),
        label = "snowfall-cycle",
    )

    Canvas(modifier.fillMaxSize()) {
        val margin = 8.dp.toPx()
        repeat(SNOWFLAKE_COUNT) { index ->
            val phase = ((index * 37 + 11) % 101) / 101f
            // Integer speeds ensure every flake is continuous at the 36-second cycle boundary.
            val progress = snowflakeProgress(cycle.value, 2 + index % 3, phase)
            val xFraction = ((index * 43 + 17) % 101) / 101f
            val drift = sin((progress * 2f * PI + index).toFloat()) * 4.dp.toPx()
            val x = size.width * xFraction + drift
            val y = -margin + progress * (size.height + margin * 2f)
            val radius = (2f + (index % 4) * .5f).dp.toPx()
            val color = Color.White.copy(alpha = .18f + (index % 5) * .045f)
            val strokeWidth = .65.dp.toPx()
            repeat(3) { axis ->
                val angle = axis * PI / 3.0
                val dx = (cos(angle) * radius).toFloat()
                val dy = (sin(angle) * radius).toFloat()
                drawLine(color, Offset(x - dx, y - dy), Offset(x + dx, y + dy), strokeWidth)
            }
        }
    }
}
