package com.impostor.data

actual fun platformStoreBillingClient(): StoreBillingClient = unavailableStoreBillingClient()

actual fun platformEntitlementStore(): EntitlementStore = fallbackEntitlementStore()

actual fun platformTrialStore(): TrialStore = fallbackTrialStore()

actual fun initializePlatformStoreBilling(context: Any) = Unit