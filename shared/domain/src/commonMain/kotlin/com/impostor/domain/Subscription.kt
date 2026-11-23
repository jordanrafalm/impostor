package com.impostor.domain

import kotlinx.coroutines.flow.Flow

const val PremiumEntitlementKey = "premium_all_categories"
const val PremiumMonthlyProductId = "com.impostor.app.premium.monthly"
const val PremiumYearlyProductId = "com.impostor.app.premium.yearly"

enum class SubscriptionPlan(val productId: String) {
    MONTHLY(PremiumMonthlyProductId),
    YEARLY(PremiumYearlyProductId);

    companion object {
        fun fromProductId(productId: String): SubscriptionPlan? =
            entries.firstOrNull { it.productId == productId }
    }
}

enum class SubscriptionState {
    LOCKED,
    PURCHASING,
    ACTIVE,
    EXPIRED,
    REVOKED,
    RESTORING,
    UNAVAILABLE,
}

enum class EntitlementSource {
    STORE,
    LOCAL_CACHE,
}

data class SubscriptionProduct(
    val plan: SubscriptionPlan,
    val productId: String,
    val title: String,
    val formattedPrice: String,
    val periodLabel: String,
)

data class Entitlement(
    val key: String = PremiumEntitlementKey,
    val state: SubscriptionState,
    val expiresAtMillis: Long? = null,
    val source: EntitlementSource,
)

/** A transient store proof for the notice, never an entitlement or a persisted transaction. */
sealed interface MonthlyPriceNoticeEligibility {
    data class Eligible(val renewalPrice: String, val currentPrice: String) : MonthlyPriceNoticeEligibility
    data object Ineligible : MonthlyPriceNoticeEligibility
    data object Unknown : MonthlyPriceNoticeEligibility
}

enum class SubscriptionError {
    NETWORK_UNAVAILABLE,
    STORE_UNAVAILABLE,
    PRODUCT_UNAVAILABLE,
    PURCHASE_CANCELLED,
    PURCHASE_PENDING,
    ALREADY_OWNED,
    PURCHASE_FAILED,
    RESTORE_FAILED,
}

class SubscriptionException(val error: SubscriptionError) : IllegalStateException(error.name)

interface SubscriptionRepository {
    val entitlement: Flow<Entitlement>
    suspend fun monthlyPriceNoticeEligibility(): MonthlyPriceNoticeEligibility
    suspend fun loadProducts(): Result<List<SubscriptionProduct>>
    suspend fun reconcileEntitlement(): Result<Entitlement>
    suspend fun purchase(plan: SubscriptionPlan): Result<Entitlement>
    suspend fun restorePurchases(): Result<Entitlement>
    suspend fun openManageSubscriptions(): Result<Unit>
}

class PurchasePremiumUseCase(private val repository: SubscriptionRepository) {
    suspend fun invoke(plan: SubscriptionPlan): Result<Entitlement> = repository.purchase(plan)
}

class RestorePurchasesUseCase(private val repository: SubscriptionRepository) {
    suspend fun invoke(): Result<Entitlement> = repository.restorePurchases()
}

fun Entitlement.unlocksAllCategories(): Boolean =
    state == SubscriptionState.ACTIVE && source == EntitlementSource.STORE
