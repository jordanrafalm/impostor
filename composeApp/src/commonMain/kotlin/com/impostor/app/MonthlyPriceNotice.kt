package com.impostor.app

import com.impostor.domain.MonthlyPriceNoticeEligibility

internal class MonthlyPriceNoticePreferences(private val store: PreferenceStore) {
    fun hasAcknowledged(): Boolean = store.getBoolean(PreferenceKeys.MONTHLY_PRICE_NOTICE_ACKNOWLEDGED)

    fun acknowledge() {
        store.putBoolean(PreferenceKeys.MONTHLY_PRICE_NOTICE_ACKNOWLEDGED, true)
    }
}

internal fun shouldShowMonthlyPriceNotice(
    eligibility: MonthlyPriceNoticeEligibility,
    acknowledged: Boolean,
    dismissedThisLaunch: Boolean,
    deferredThisLaunch: Boolean,
    isHome: Boolean,
    isForeground: Boolean,
    startupPromptChecked: Boolean,
    hasBlockingPrompt: Boolean,
    notificationPermissionPromptStatus: NotificationPermissionPromptStatus,
): Boolean =
    eligibility is MonthlyPriceNoticeEligibility.Eligible &&
        !acknowledged &&
        !dismissedThisLaunch &&
        !deferredThisLaunch &&
        isHome &&
        isForeground &&
        startupPromptChecked &&
        !hasBlockingPrompt &&
        notificationPermissionPromptStatus == NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED

internal fun shouldDeferMonthlyPriceNoticeForNotificationPrompt(
    status: NotificationPermissionPromptStatus,
): Boolean = status == NotificationPermissionPromptStatus.PROMPT_REQUIRED_OR_IN_PROGRESS
