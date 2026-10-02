package com.impostor.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest

internal class TrialRepositoryTest {
    @Test
    fun `should count each started session once and stop at the trial limit`() = runTest {
        // Given
        val store = FakeTrialStore(CachedTrialState(4, null))
        val repository = TrialRepositoryImpl(store)

        // When
        val fifthGame = repository.recordStartedGame("game-5")
        val repeatedFifthGame = repository.recordStartedGame("game-5")
        val sixthGame = repository.recordStartedGame("game-6")

        // Then
        assertEquals(5, fifthGame.startedGames)
        assertEquals(fifthGame, repeatedFifthGame)
        assertEquals(5, sixthGame.startedGames)
        assertEquals(5, store.value.startedGames)
    }

    @Test
    fun `should not lose a started game when sessions start concurrently`() = runTest {
        // Given
        val store = FakeTrialStore(CachedTrialState(0, null))
        val repository = TrialRepositoryImpl(store)

        // When
        val results = listOf("game-1", "game-2").map { sessionId ->
            async { repository.recordStartedGame(sessionId) }
        }.awaitAll()

        // Then
        assertEquals(2, store.value.startedGames)
        assertEquals(2, results.maxOf { it.startedGames })
    }

    @Test
    fun `should preserve five used games across repository recreation`() = runTest {
        val store = FakeTrialStore(CachedTrialState(0, null))
        repeat(5) { index ->
            val repository = TrialRepositoryImpl(store)
            assertEquals(5 - index, repository.state.value.remainingFreeGames)
            repository.recordStartedGame("game-$index")
        }
        val reopened = TrialRepositoryImpl(store)
        assertEquals(0, reopened.state.value.remainingFreeGames)
        kotlin.test.assertFalse(reopened.state.value.hasFreeCategories)
    }

    private class FakeTrialStore(initial: CachedTrialState) : TrialStore {
        var value = initial

        override fun read(): CachedTrialState = value

        override fun write(state: CachedTrialState) {
            value = state
        }
    }
}