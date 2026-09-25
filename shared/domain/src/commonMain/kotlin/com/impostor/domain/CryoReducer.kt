package com.impostor.domain

sealed interface CryoState {
    val progress: Float

    data object Idle : CryoState { override val progress = 0f }
    data class Melting(override val progress: Float, val x: Float, val y: Float) : CryoState
    data object Revealed : CryoState { override val progress = 1f }
    data class FlashFreezing(override val progress: Float) : CryoState
}

sealed interface CryoIntent {
    data class PointerDown(val x: Float, val y: Float) : CryoIntent
    data class PointerMoved(val x: Float, val y: Float) : CryoIntent
    data object PointerUp : CryoIntent
    data object PointerCancel : CryoIntent
    data class Frame(val elapsedMillis: Long) : CryoIntent
}

class CryoReducer(
    private val revealThreshold: Float = 0.6f,
    private val meltDurationMillis: Long = 1_000L,
    private val freezeDurationMillis: Long = 380L,
) {
    fun reduce(state: CryoState, intent: CryoIntent): CryoState = when (intent) {
        is CryoIntent.PointerDown -> when (state) {
            CryoState.Idle -> CryoState.Melting(0f, intent.x, intent.y)
            else -> state
        }
        is CryoIntent.PointerMoved -> when (state) {
            is CryoState.Melting -> state.copy(x = intent.x, y = intent.y)
            else -> state
        }
        CryoIntent.PointerUp, CryoIntent.PointerCancel -> when (state) {
            is CryoState.Melting, CryoState.Revealed -> CryoState.FlashFreezing(1f)
            else -> state
        }
        is CryoIntent.Frame -> when (state) {
            is CryoState.Melting -> {
                val next = (state.progress + intent.elapsedMillis.toFloat() / meltDurationMillis).coerceIn(0f, 1f)
                if (next >= 1f) CryoState.Revealed else state.copy(progress = next)
            }
            is CryoState.FlashFreezing -> {
                val next = (state.progress - intent.elapsedMillis.toFloat() / freezeDurationMillis).coerceAtLeast(0f)
                if (next == 0f) CryoState.Idle else CryoState.FlashFreezing(next)
            }
            else -> state
        }
    }
}