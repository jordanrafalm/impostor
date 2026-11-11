package com.impostor.domain

import kotlinx.coroutines.flow.StateFlow

const val FreeTrialGamesLimit = 5
const val BlackFridayGamesLimit = 5

/** Categories that are never gated behind the trial limit or Premium. */
val AlwaysFreeCategoryIds = setOf("animals", "places", "sports")

data class TrialState(
    val startedGames: Int,
    val blackFridayClaimed: Boolean = false,
    val blackFridayGamesUsed: Int = 0,
) {
    val remainingFreeGames: Int
        get() = (FreeTrialGamesLimit - startedGames).coerceAtLeast(0) +
            if (blackFridayClaimed) (BlackFridayGamesLimit - blackFridayGamesUsed).coerceAtLeast(0) else 0

    val hasFreeCategories: Boolean
        get() = remainingFreeGames > 0
}

interface TrialRepository {
    val state: StateFlow<TrialState>

    suspend fun recordStartedGame(sessionId: String): TrialState
    suspend fun claimBlackFriday(): TrialState
}

class RecordStartedGameUseCase(private val repository: TrialRepository) {
    suspend fun invoke(sessionId: String): TrialState = repository.recordStartedGame(sessionId)
}

fun TrialState.canStartGame(entitlement: Entitlement): Boolean =
    hasFreeCategories || entitlement.unlocksAllCategories()

fun requiresTrialGame(categoryIds: Set<String>, entitlement: Entitlement): Boolean =
    !entitlement.unlocksAllCategories() && categoryIds.any { it !in AlwaysFreeCategoryIds }
