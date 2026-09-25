package com.impostor.domain

import kotlinx.coroutines.flow.Flow

const val PremiumEntitlementKey = "premium_all_categories"
const val PremiumProductId = "com.impostor.app.premium.monthly"

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

enum class SubscriptionError {
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
    suspend fun loadProduct(): Result<SubscriptionProduct>
    suspend fun purchase(): Result<Entitlement>
    suspend fun restorePurchases(): Result<Entitlement>
    suspend fun openManageSubscriptions(): Result<Unit>
}

class PurchasePremiumUseCase(private val repository: SubscriptionRepository) {
    suspend fun invoke(): Result<Entitlement> = repository.purchase()
}

class RestorePurchasesUseCase(private val repository: SubscriptionRepository) {
    suspend fun invoke(): Result<Entitlement> = repository.restorePurchases()
}

fun Entitlement.unlocksAllCategories(): Boolean =
    state == SubscriptionState.ACTIVE && source == EntitlementSource.STORE
