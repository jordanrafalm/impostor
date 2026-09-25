package com.impostor.data

import com.impostor.domain.AnalyticsEvent
import com.impostor.domain.AnalyticsService
import com.impostor.domain.RemoteCode
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.toMilliseconds
import kotlinx.serialization.Serializable

@Serializable
internal data class PromoCodeDocument(
    val enabled: Boolean = false,
    val unlockAllCategories: Boolean = false,
    val categoryIds: List<String> = emptyList(),
    val expiresAt: Timestamp? = null,
)

internal fun PromoCodeDocument.toRemoteCode(code: String): RemoteCode = RemoteCode(
    value = code,
    categoryIds = categoryIds.toSet(),
    enabled = enabled,
    unlockAllCategories = unlockAllCategories,
    expiresAtMillis = expiresAt?.toMilliseconds()?.toLong(),
)

internal class FirestoreRemoteCodeSource(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : RemoteCodeSource {
    override suspend fun fetchCodes(): List<RemoteCode>? = runCatching {
        firestore.collection("promoCodes").get().documents.mapNotNull { document ->
            if (!document.exists) return@mapNotNull null
            runCatching { document.data<PromoCodeDocument>().toRemoteCode(document.id) }.getOrNull()
        }
    }.getOrNull()
}

internal class FirebaseAnalyticsService : AnalyticsService {
    override fun log(event: AnalyticsEvent, parameters: Map<String, String>) {
        runCatching {
            when (event) {
                AnalyticsEvent.CODES_OPENED -> Firebase.analytics.logEvent("codes_opened")
                AnalyticsEvent.CODE_VALIDATED -> {
                    val result = parameters["result"] ?: return@runCatching
                    Firebase.analytics.logEvent("code_validated", mapOf("result" to result))
                }
                AnalyticsEvent.PREMIUM_SCREEN_OPENED -> Firebase.analytics.logEvent("premium_screen_opened")
                AnalyticsEvent.SUBSCRIPTION_PRODUCT_LOADED -> Firebase.analytics.logEvent("subscription_product_loaded")
                AnalyticsEvent.SUBSCRIPTION_PURCHASE_STARTED -> Firebase.analytics.logEvent("subscription_purchase_started")
                AnalyticsEvent.SUBSCRIPTION_PURCHASE_RESULT -> {
                    Firebase.analytics.logEvent("subscription_purchase_result", mapOf("result" to safeResult(parameters)))
                }
                AnalyticsEvent.SUBSCRIPTION_RESTORE_RESULT -> {
                    Firebase.analytics.logEvent("subscription_restore_result", mapOf("result" to safeResult(parameters)))
                }
                AnalyticsEvent.PREMIUM_ENTITLEMENT_CHANGED -> {
                    Firebase.analytics.logEvent("premium_entitlement_changed", mapOf("state" to safeResult(parameters)))
                }
            }
        }
    }

    private fun safeResult(parameters: Map<String, String>): String =
        parameters["result"]?.takeIf { it in setOf("success", "cancelled", "pending", "failed", "active", "none", "expired", "revoked", "locked") }
            ?: parameters["state"]?.takeIf { it in setOf("active", "expired", "revoked", "locked") }
            ?: "failed"
}