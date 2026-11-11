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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

private val DeepSea = Color(0xFF050B1F)
private val Ocean = Color(0xFF0A1633)
private val AuroraBlue = Color(0xFF7FD4FF)
private val AuroraCyan = Color(0xFF00C8FF)

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

    Canvas(modifier.fillMaxSize()) {
        drawOceanBase()
        drawAurora(auroraProgress)
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
