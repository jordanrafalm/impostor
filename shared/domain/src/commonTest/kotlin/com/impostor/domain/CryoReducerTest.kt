package com.impostor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CryoReducerTest {
    private val reducer = CryoReducer()

    @Test
    fun roleIsNotSemanticUntilThresholdAndReleaseHidesImmediately() {
        var state: CryoState = reducer.reduce(CryoState.Idle, CryoIntent.PointerDown(0.5f, 0.5f))
        state = reducer.reduce(state, CryoIntent.Frame(500))
        assertIs<CryoState.Melting>(state)
        state = reducer.reduce(state, CryoIntent.PointerUp)
        assertIs<CryoState.FlashFreezing>(state)
    }

    @Test
    fun holdRevealsAndFreezeReturnsToIdle() {
        var state: CryoState = reducer.reduce(CryoState.Idle, CryoIntent.PointerDown(0f, 0f))
        state = reducer.reduce(state, CryoIntent.Frame(600))
        assertIs<CryoState.Melting>(state)
        state = reducer.reduce(state, CryoIntent.Frame(400))
        assertEquals(CryoState.Revealed, state)
        state = reducer.reduce(state, CryoIntent.PointerCancel)
        assertEquals(CryoState.FlashFreezing(1f), state)
        state = reducer.reduce(state, CryoIntent.PointerDown(0f, 0f))
        assertEquals(CryoState.FlashFreezing(1f), state)
        state = reducer.reduce(state, CryoIntent.PointerUp)
        assertEquals(CryoState.FlashFreezing(1f), state)
        state = reducer.reduce(state, CryoIntent.Frame(380))
        assertEquals(CryoState.Idle, state)
    }
}