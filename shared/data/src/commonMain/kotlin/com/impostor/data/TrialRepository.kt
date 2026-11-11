package com.impostor.data

import com.impostor.domain.FreeTrialGamesLimit
import com.impostor.domain.BlackFridayGamesLimit
import com.impostor.domain.TrialRepository
import com.impostor.domain.TrialState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CachedTrialState(
    val startedGames: Int,
    val lastStartedSessionId: String?,
    val blackFridayClaimed: Boolean = false,
    val blackFridayGamesUsed: Int = 0,
    val countedSessionIds: Set<String> = emptySet(),
)

internal fun encodeTrialState(state: CachedTrialState): String = Json.encodeToString(state)
internal fun decodeTrialState(value: String): CachedTrialState = Json.decodeFromString(value)

interface TrialStore {
    fun read(): CachedTrialState
    fun write(state: CachedTrialState)
}

private class InMemoryTrialStore : TrialStore {
    private var value = CachedTrialState(0, null)

    override fun read(): CachedTrialState = value

    override fun write(state: CachedTrialState) {
        value = state
    }
}

private val fallbackTrialStore = InMemoryTrialStore()

expect fun platformTrialStore(): TrialStore

class TrialRepositoryImpl(
    private val store: TrialStore = platformTrialStore(),
) : TrialRepository {
    private val mutex = Mutex()
    private val initial = store.read().toDomain()
    private val _state = MutableStateFlow(initial)

    override val state: StateFlow<TrialState> = _state.asStateFlow()

    override suspend fun recordStartedGame(sessionId: String): TrialState = mutex.withLock {
        val cached = store.read()
        if (cached.lastStartedSessionId == sessionId || sessionId in cached.countedSessionIds) {
            return cached.toDomain().also { _state.value = it }
        }

        val useTrial = cached.startedGames < FreeTrialGamesLimit
        val usePromotion = !useTrial && cached.blackFridayClaimed &&
            cached.blackFridayGamesUsed < BlackFridayGamesLimit
        if (!useTrial && !usePromotion) return cached.toDomain().also { _state.value = it }
        val next = cached.copy(
            startedGames = if (useTrial) cached.startedGames + 1 else cached.startedGames,
            blackFridayGamesUsed = if (usePromotion) cached.blackFridayGamesUsed + 1 else cached.blackFridayGamesUsed,
            lastStartedSessionId = sessionId,
            countedSessionIds = cached.countedSessionIds + sessionId +
                listOfNotNull(cached.lastStartedSessionId),
        )
        store.write(next)
        return next.toDomain().also { _state.value = it }
    }

    override suspend fun claimBlackFriday(): TrialState = mutex.withLock {
        val cached = store.read()
        val next = if (cached.blackFridayClaimed) cached else cached.copy(blackFridayClaimed = true)
        if (next != cached) store.write(next)
        next.toDomain().also { _state.value = it }
    }
}

private fun CachedTrialState.toDomain() = TrialState(
    startedGames = startedGames.coerceIn(0, FreeTrialGamesLimit),
    blackFridayClaimed = blackFridayClaimed,
    blackFridayGamesUsed = blackFridayGamesUsed.coerceIn(0, BlackFridayGamesLimit),
)

internal fun fallbackTrialStore(): TrialStore = fallbackTrialStore