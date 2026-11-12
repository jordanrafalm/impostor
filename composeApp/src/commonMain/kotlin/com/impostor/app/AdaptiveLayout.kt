package com.impostor.app

internal fun hasTwoPanes(width: Float, minPaneWidth: Float, horizontalGaps: Float): Boolean =
    width >= minPaneWidth * 2 + horizontalGaps

internal fun categoryColumnCount(width: Float): Int = when {
    width >= 474f -> 3
    width >= 312f -> 2
    else -> 1
}

internal fun canShowRevealSecret(holding: Boolean, active: Boolean, identityVisible: Boolean): Boolean =
    holding && active && !identityVisible
