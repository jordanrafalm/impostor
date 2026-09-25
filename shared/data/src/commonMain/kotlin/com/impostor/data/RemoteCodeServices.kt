package com.impostor.data

import com.impostor.domain.AnalyticsEvent
import com.impostor.domain.AnalyticsService
import com.impostor.domain.CodeRepository
import com.impostor.domain.CodeValidationResult
import com.impostor.domain.RemoteCode
import com.impostor.domain.isValidAt

interface RemoteCodeSource {
    suspend fun fetchCodes(): List<RemoteCode>?
}

@OptIn(kotlin.time.ExperimentalTime::class)
class CachedCodeRepository(
    private val remote: RemoteCodeSource,
    private val nowMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : CodeRepository {
    private var cachedCodes: List<RemoteCode> = emptyList()

    override suspend fun validate(code: String): CodeValidationResult {
        val knownCodes = if (cachedCodes.isNotEmpty()) cachedCodes else {
            runCatching { remote.fetchCodes() }.getOrNull()?.also { cachedCodes = it } ?: cachedCodes
        }
        val matchingCode = knownCodes.firstOrNull {
            it.value.trim().uppercase() == code.trim().uppercase() && it.isValidAt(nowMillis())
        }
            ?: return if (knownCodes.isEmpty()) CodeValidationResult.Unavailable else CodeValidationResult.Invalid
        return CodeValidationResult.Accepted(matchingCode)
    }
}

fun platformRemoteCodeSource(): RemoteCodeSource = FirestoreRemoteCodeSource()

fun codeRepository(): CodeRepository = CachedCodeRepository(platformRemoteCodeSource())

fun platformAnalyticsService(): AnalyticsService = FirebaseAnalyticsService()

fun analyticsService(): AnalyticsService = platformAnalyticsService()