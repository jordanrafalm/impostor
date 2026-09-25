package com.impostor.data

import com.impostor.domain.CodeValidationResult
import com.impostor.domain.RemoteCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

internal class CachedCodeRepositoryTest {
    @Test
    fun `should use cached codes when remote becomes unavailable`() = runTest {
        var available = true
        val remote = object : RemoteCodeSource {
            override suspend fun fetchCodes(): List<RemoteCode>? =
                if (available) listOf(RemoteCode("LOCAL", setOf("future"))) else null
        }
        val repository = CachedCodeRepository(remote)

        assertTrue(repository.validate("LOCAL") is CodeValidationResult.Accepted)
        available = false

        assertEquals(CodeValidationResult.Accepted(RemoteCode("LOCAL", setOf("future"))), repository.validate("LOCAL"))
    }

    @Test
    fun `should accept normalized enabled all-category code`() = runTest {
        val remote = object : RemoteCodeSource {
            override suspend fun fetchCodes(): List<RemoteCode> = listOf(
                RemoteCode("ALL", emptySet(), unlockAllCategories = true),
            )
        }
        val repository = CachedCodeRepository(remote, nowMillis = { 0 })

        val result = repository.validate("  all ")

        assertEquals(true, (result as CodeValidationResult.Accepted).code.unlockAllCategories)
    }

    @Test
    fun `should return invalid for expired cached code`() = runTest {
        val remote = object : RemoteCodeSource {
            override suspend fun fetchCodes(): List<RemoteCode> = listOf(
                RemoteCode("OLD", setOf("future"), expiresAtMillis = 100),
            )
        }
        val repository = CachedCodeRepository(remote, nowMillis = { 100 })

        assertEquals(CodeValidationResult.Invalid, repository.validate("OLD"))
    }
}