package com.impostor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

internal class ValidateCodeUseCaseTest {
    @Test
    fun `should normalize code before validation`() = runTest {
        val repository = object : CodeRepository {
            override suspend fun validate(code: String): CodeValidationResult =
                CodeValidationResult.Accepted(RemoteCode(code, setOf("future")))
        }

        val result = ValidateCodeUseCase(repository).invoke("  winter  ")

        assertEquals("WINTER", (result as CodeValidationResult.Accepted).code.value)
    }
}