package com.impostor.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

private val DeepSea = Color(0xFF050B1F)
private val Ocean = Color(0xFF0A1633)
private val AuroraBlue = Color(0xFF7FD4FF)
private val AuroraCyan = Color(0xFF00C8FF)

private data class Drip(
    val xFraction: Float,
    val heightDp: Float,
    val widthDp: Float,
    val phase: Float,
)

private val drips = List(18) { index ->
    Drip(
        xFraction = ((index * 55 + (index % 3) * 40) % 1_000) / 1_000f,
        heightDp = 18f + (index % 5) * 12f,
        widthDp = 1.5f + (index % 3) * .65f,
        phase = ((index * 9) % 80) / 80f,
    )
}

/** Edge-to-edge animated ice-and-aurora backdrop shared by every app route. */
@Composable
fun IceBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ice-backdrop")
    val auroraProgress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aurora-drift",
    ).value
    val dripProgress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = FastOutSlowInEasing),
        ),
        label = "water-drips",
    ).value

    Canvas(modifier.fillMaxSize()) {
        drawOceanBase()
        drawAurora(auroraProgress)
        drawDrips(dripProgress)
        drawFrostGrain()
        drawVignette()
    }
}

private fun DrawScope.drawOceanBase() {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF12275C), Ocean, DeepSea),
            center = Offset(size.width * .5f, -size.height * .1f),
            radius = size.maxDimension * 1.2f,
        ),
    )
}

private fun DrawScope.drawAurora(progress: Float) {
    val xShift = (progress - .5f) * size.width * .12f
    val yShift = (progress - .5f) * size.height * .08f
    drawCircle(
        brush = Brush.radialGradient(listOf(AuroraBlue.copy(alpha = .48f), Color.Transparent)),
        radius = size.maxDimension * .42f,
        center = Offset(size.width * .2f + xShift, size.height * .15f + yShift),
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFF4078FF).copy(alpha = .40f), Color.Transparent)),
        radius = size.maxDimension * .46f,
        center = Offset(size.width * .85f - xShift, size.height * .25f - yShift),
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(AuroraCyan.copy(alpha = .28f), Color.Transparent)),
        radius = size.maxDimension * .5f,
        center = Offset(size.width * .6f + xShift, size.height * .9f - yShift),
    )
}

private fun DrawScope.drawDrips(progress: Float) {
    drips.forEach { drip ->
        val travel = (progress + drip.phase) % 1f
        val height = drip.heightDp.dp.toPx()
        val width = drip.widthDp.dp.toPx()
        val y = -height + travel * (size.height + height)
        val alpha = when {
            travel < .08f -> travel / .08f
            travel > .85f -> (1f - travel) / .15f
            else -> 1f
        } * .72f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, AuroraBlue.copy(alpha = alpha)),
                startY = y,
                endY = y + height,
            ),
            topLeft = Offset(size.width * drip.xFraction, y),
            size = Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width, width),
        )
    }
}

private fun DrawScope.drawFrostGrain() {
    repeat(140) { index ->
        val x = ((index * 37) % 101) / 100f * size.width
        val y = ((index * 61 + 17) % 103) / 102f * size.height
        val radius = if (index % 4 == 0) .9.dp.toPx() else .45.dp.toPx()
        drawCircle(Color.White.copy(alpha = .025f), radius, Offset(x, y))
    }
}

private fun DrawScope.drawVignette() {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, DeepSea.copy(alpha = .78f)),
            center = Offset(size.width * .5f, size.height * .42f),
            radius = size.maxDimension * .76f,
        ),
    )
}
