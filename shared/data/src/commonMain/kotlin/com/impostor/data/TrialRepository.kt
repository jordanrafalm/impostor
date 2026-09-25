package com.impostor.data

import com.impostor.domain.FreeTrialGamesLimit
import com.impostor.domain.TrialRepository
import com.impostor.domain.TrialState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CachedTrialState(
    val startedGames: Int,
    val lastStartedSessionId: String?,
)

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
        if (cached.lastStartedSessionId == sessionId) return _state.value

        val next = CachedTrialState(
            startedGames = (cached.startedGames + 1).coerceAtMost(FreeTrialGamesLimit),
            lastStartedSessionId = sessionId,
        )
        store.write(next)
        return next.toDomain().also { _state.value = it }
    }
}

private fun CachedTrialState.toDomain() = TrialState(startedGames.coerceIn(0, FreeTrialGamesLimit))

internal fun fallbackTrialStore(): TrialStore = fallbackTrialStore