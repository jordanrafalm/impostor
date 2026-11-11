package com.impostor.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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

    @Test
    fun `should grant once and spend existing trial games before promotion`() = runTest {
        // Given
        val store = FakeTrialStore(CachedTrialState(3, "legacy"))
        var sut = TrialRepositoryImpl(store)

        // When
        sut.claimBlackFriday()
        sut.recordStartedGame("trial-4")
        sut.recordStartedGame("trial-5")
        assertEquals(0, store.value.blackFridayGamesUsed)
        sut.recordStartedGame("promo-1")
        sut = TrialRepositoryImpl(store)
        sut.claimBlackFriday()

        // Then
        assertTrue(sut.state.value.blackFridayClaimed)
        assertEquals(5, sut.state.value.startedGames)
        assertEquals(1, sut.state.value.blackFridayGamesUsed)
        assertEquals(4, sut.state.value.remainingFreeGames)
    }

    @Test
    fun `should not count a previously recorded session after another session or recreation`() = runTest {
        // Given
        val store = FakeTrialStore(CachedTrialState(5, "old-session"))
        val sut = TrialRepositoryImpl(store)
        sut.claimBlackFriday()
        sut.recordStartedGame("promo-1")
        sut.recordStartedGame("promo-2")

        // When
        val reopened = TrialRepositoryImpl(store)
        reopened.recordStartedGame("promo-1")
        reopened.recordStartedGame("old-session")

        // Then
        assertEquals(2, reopened.state.value.blackFridayGamesUsed)
        assertEquals(3, reopened.state.value.remainingFreeGames)
    }

    @Test
    fun `should exhaust promotion without resetting it on another claim`() = runTest {
        // Given
        val store = FakeTrialStore(CachedTrialState(5, null))
        val sut = TrialRepositoryImpl(store)
        sut.claimBlackFriday()

        // When
        repeat(5) { sut.recordStartedGame("promo-$it") }
        sut.recordStartedGame("extra")
        sut.claimBlackFriday()

        // Then
        assertEquals(5, store.value.blackFridayGamesUsed)
        assertFalse(sut.state.value.hasFreeCategories)
        assertEquals(0, sut.state.value.remainingFreeGames)
    }

    @Test
    fun `should serialize the full state without changing the legacy trial values`() {
        // Given
        val state = CachedTrialState(4, "legacy", true, 1, setOf("legacy", "new"))

        // When
        val restored = decodeTrialState(encodeTrialState(state))

        // Then
        assertEquals(state, restored)
    }

    @Test
    fun `should count a session once when it is recorded concurrently`() = runTest {
        // Given
        val store = FakeTrialStore(CachedTrialState(5, null))
        val sut = TrialRepositoryImpl(store)
        sut.claimBlackFriday()

        // When
        List(4) { async { sut.recordStartedGame("same-session") } }.awaitAll()

        // Then
        assertEquals(1, sut.state.value.blackFridayGamesUsed)
    }

    @Test
    fun `should preserve legacy last session when granting the promotion`() = runTest {
        // Given
        val store = FakeTrialStore(CachedTrialState(4, "legacy"))
        val sut = TrialRepositoryImpl(store)

        // When
        sut.claimBlackFriday()
        sut.recordStartedGame("legacy")

        // Then
        assertEquals(4, store.value.startedGames)
        assertEquals(0, store.value.blackFridayGamesUsed)
        assertEquals("legacy", store.value.lastStartedSessionId)
    }

    private class FakeTrialStore(initial: CachedTrialState) : TrialStore {
        var value = initial

        override fun read(): CachedTrialState = value

        override fun write(state: CachedTrialState) {
            value = state
        }
    }
}