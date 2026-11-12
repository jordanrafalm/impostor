package com.impostor.domain

data class RemoteCode(
    val value: String,
    val categoryIds: Set<String>,
    val enabled: Boolean = true,
    val unlockAllCategories: Boolean = false,
    val expiresAtMillis: Long? = null,
)

fun RemoteCode.isValidAt(nowMillis: Long): Boolean =
    enabled &&
        (expiresAtMillis == null || expiresAtMillis > nowMillis) &&
        (unlockAllCategories || categoryIds.isNotEmpty())

fun RemoteCode.entitledCategoryIds(activeCategoryIds: Set<String>): Set<String> =
    if (unlockAllCategories) activeCategoryIds else categoryIds intersect activeCategoryIds

sealed interface CodeValidationResult {
    data class Accepted(val code: RemoteCode) : CodeValidationResult
    data object Invalid : CodeValidationResult
    data object Unavailable : CodeValidationResult
}

interface CodeRepository {
    suspend fun validate(code: String): CodeValidationResult
}

enum class AnalyticsEvent {
    CODES_OPENED,
    CODE_VALIDATED,
    PREMIUM_SCREEN_OPENED,
    SUBSCRIPTION_PRODUCT_LOADED,
    SUBSCRIPTION_PURCHASE_STARTED,
    SUBSCRIPTION_PURCHASE_RESULT,
    SUBSCRIPTION_RESTORE_RESULT,
    PREMIUM_ENTITLEMENT_CHANGED,
    GAME_STARTED_CATEGORY,
    BLACK_FRIDAY_ACCEPTED,
    BLACK_FRIDAY_CANCELLED,
    REVIEW_PROMPT_SHOWN,
    REVIEW_PROMPT_RATE_CLICKED,
    REVIEW_PROMPT_DISMISSED,
}

interface AnalyticsService {
    fun log(event: AnalyticsEvent, parameters: Map<String, String> = emptyMap())
}

class ValidateCodeUseCase(private val repository: CodeRepository) {
    suspend fun invoke(code: String): CodeValidationResult {
        val normalizedCode = code.trim().uppercase()
        if (normalizedCode.isEmpty()) return CodeValidationResult.Invalid
        return repository.validate(normalizedCode)
    }
}