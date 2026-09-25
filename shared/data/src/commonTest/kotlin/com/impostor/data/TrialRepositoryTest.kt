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
        val store = FakeTrialStore(CachedTrialState(2, null))
        val repository = TrialRepositoryImpl(store)

        // When
        val thirdGame = repository.recordStartedGame("game-3")
        val repeatedThirdGame = repository.recordStartedGame("game-3")
        val fourthGame = repository.recordStartedGame("game-4")

        // Then
        assertEquals(3, thirdGame.startedGames)
        assertEquals(thirdGame, repeatedThirdGame)
        assertEquals(3, fourthGame.startedGames)
        assertEquals(3, store.value.startedGames)
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
    fun `should preserve three used games across repository recreation`() = runTest {
        val store = FakeTrialStore(CachedTrialState(0, null))
        repeat(3) { index ->
            val repository = TrialRepositoryImpl(store)
            assertEquals(3 - index, repository.state.value.remainingFreeGames)
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