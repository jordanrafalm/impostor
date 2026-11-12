package com.impostor.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeScreenVisibilityTest {
    // The current Home always exposes the premium action; the former conditional
    // visibility policy was removed before the adaptive layout was introduced.
    @Test
    fun `should use a single column when the home panes do not fit`() {
        assertFalse(hasTwoPanes(631f, minPaneWidth = 280f, horizontalGaps = 72f))
    }

    @Test
    fun `should use two columns when both home panes fit`() {
        assertTrue(hasTwoPanes(632f, minPaneWidth = 280f, horizontalGaps = 72f))
    }
}
