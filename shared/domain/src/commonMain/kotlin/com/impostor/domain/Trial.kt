package com.impostor.domain

import kotlinx.coroutines.flow.StateFlow

const val FreeTrialGamesLimit = 3

data class TrialState(
    val startedGames: Int,
) {
    val remainingFreeGames: Int
        get() = (FreeTrialGamesLimit - startedGames).coerceAtLeast(0)

    val hasFreeCategories: Boolean
        get() = startedGames < FreeTrialGamesLimit
}

interface TrialRepository {
    val state: StateFlow<TrialState>

    suspend fun recordStartedGame(sessionId: String): TrialState
}

class RecordStartedGameUseCase(private val repository: TrialRepository) {
    suspend fun invoke(sessionId: String): TrialState = repository.recordStartedGame(sessionId)
}

fun TrialState.canStartGame(entitlement: Entitlement): Boolean =
    hasFreeCategories || entitlement.unlocksAllCategories()
