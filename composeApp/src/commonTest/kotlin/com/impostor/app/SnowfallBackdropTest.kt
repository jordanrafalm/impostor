package com.impostor.app

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

internal class SnowfallBackdropTest {
    @Test
    fun `should preserve flake positions when the overall cycle wraps`() {
        // Given
        val phase = .21f

        // When
        val before = (2..4).map { snowflakeProgress(.99999f, it, phase) }
        val after = (2..4).map { snowflakeProgress(0f, it, phase) }

        // Then
        assertTrue(before.zip(after).all { (left, right) -> abs(left - right) < .001f })
    }

    @Test
    fun `should stagger flakes with different phases`() {
        // Given
        val firstPhase = .1f
        val secondPhase = .4f

        // When
        val first = snowflakeProgress(.2f, 2, firstPhase)
        val second = snowflakeProgress(.2f, 2, secondPhase)

        // Then
        assertTrue(abs(first - second) > .1f)
    }
}
