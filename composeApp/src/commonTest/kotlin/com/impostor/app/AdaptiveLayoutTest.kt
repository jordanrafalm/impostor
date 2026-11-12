package com.impostor.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveLayoutTest {
    @Test
    fun `should choose category columns from available width`() {
        assertEquals(1, categoryColumnCount(311f))
        assertEquals(2, categoryColumnCount(312f))
        assertEquals(2, categoryColumnCount(473f))
        assertEquals(3, categoryColumnCount(474f))
    }

    @Test
    fun `should never show a secret after release interruption or identity reset`() {
        assertTrue(canShowRevealSecret(holding = true, active = true, identityVisible = false))
        assertFalse(canShowRevealSecret(holding = false, active = true, identityVisible = false))
        assertFalse(canShowRevealSecret(holding = true, active = false, identityVisible = false))
        assertFalse(canShowRevealSecret(holding = true, active = true, identityVisible = true))
    }
}
