package com.impostor.domain

import kotlinx.coroutines.CancellationException

interface PlayerNameUploadRepository {
    suspend fun uploadNames(names: List<String>)
}

sealed interface PlayerNameUploadOutcome {
    data object Uploaded : PlayerNameUploadOutcome
    data object Failed : PlayerNameUploadOutcome
}

class UploadPlayerNamesUseCase(
    private val repository: PlayerNameUploadRepository,
) {
    suspend fun invoke(names: List<String>): PlayerNameUploadOutcome = try {
        repository.uploadNames(names.toList())
        PlayerNameUploadOutcome.Uploaded
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        PlayerNameUploadOutcome.Failed
    }
}
