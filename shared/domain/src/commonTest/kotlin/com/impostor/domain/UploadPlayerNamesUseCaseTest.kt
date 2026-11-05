package com.impostor.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class UploadPlayerNamesUseCaseTest {
    @Test
    fun `should upload only the provided player names`() = runTest {
        // Given
        val expectedNames = listOf("Ala", "Bartek", "Celina")
        val repository = RecordingPlayerNameUploadRepository()
        val sut = UploadPlayerNamesUseCase(repository)

        // When
        val outcome = sut.invoke(expectedNames)

        // Then
        assertEquals(PlayerNameUploadOutcome.Uploaded, outcome)
        assertEquals(expectedNames, repository.uploadedNames)
    }

    @Test
    fun `should return failure when the repository cannot upload names`() = runTest {
        // Given
        val repository = RecordingPlayerNameUploadRepository(fail = true)
        val sut = UploadPlayerNamesUseCase(repository)

        // When
        val outcome = sut.invoke(listOf("Ala", "Bartek", "Celina"))

        // Then
        assertEquals(PlayerNameUploadOutcome.Failed, outcome)
    }
}

private class RecordingPlayerNameUploadRepository(
    private val fail: Boolean = false,
) : PlayerNameUploadRepository {
    var uploadedNames: List<String>? = null
        private set

    override suspend fun uploadNames(names: List<String>) {
        if (fail) error("Upload unavailable")
        uploadedNames = names
    }
}
