package com.impostor.data

import com.impostor.domain.PremiumMonthlyProductId
import com.impostor.domain.PremiumYearlyProductId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FirebaseAnalyticsTest {
    @Test
    fun `subscription analytics only accepts plan labels and not store product ids`() {
        assertEquals("monthly", safeSubscriptionPlan(mapOf("plan" to "monthly")))
        assertEquals("yearly", safeSubscriptionPlan(mapOf("plan" to "yearly")))
        assertNull(safeSubscriptionPlan(mapOf("plan" to PremiumMonthlyProductId)))
        assertNull(safeSubscriptionPlan(mapOf("plan" to PremiumYearlyProductId)))
        assertNull(safeSubscriptionPlan(mapOf("plan" to "unexpected")))
        assertNull(safeSubscriptionPlan(emptyMap()))
    }
}
