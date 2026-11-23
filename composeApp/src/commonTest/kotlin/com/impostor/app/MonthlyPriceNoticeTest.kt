package com.impostor.app

import com.impostor.domain.MonthlyPriceNoticeEligibility
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MonthlyPriceNoticeTest {
    @Test
    fun `only a verified eligible result can display the notice`() {
        assertTrue(
            shouldShowMonthlyPriceNotice(
                eligibility = MonthlyPriceNoticeEligibility.Eligible("4,89 zł", "14,99 zł"),
                acknowledged = false,
                dismissedThisLaunch = false,
                deferredThisLaunch = false,
                isHome = true,
                isForeground = true,
                startupPromptChecked = true,
                hasBlockingPrompt = false,
                notificationPermissionPromptStatus = NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED,
            ),
        )
        assertFalse(shouldShow(MonthlyPriceNoticeEligibility.Ineligible))
        assertFalse(shouldShow(MonthlyPriceNoticeEligibility.Unknown))
    }

    @Test
    fun `acknowledgement persists and is not recorded merely by checking eligibility`() {
        val store = MonthlyPriceNoticeTestPreferenceStore()
        val preferences = MonthlyPriceNoticePreferences(store)

        assertFalse(preferences.hasAcknowledged())
        assertFalse(store.contains(PreferenceKeys.MONTHLY_PRICE_NOTICE_ACKNOWLEDGED))

        preferences.acknowledge()

        assertTrue(MonthlyPriceNoticePreferences(store).hasAcknowledged())
    }

    @Test
    fun `acknowledgement deferral and competing UI prevent display`() {
        val eligible = MonthlyPriceNoticeEligibility.Eligible("4,89 zł", "14,99 zł")

        assertFalse(shouldShow(eligible, acknowledged = true))
        assertFalse(shouldShow(eligible, dismissedThisLaunch = true))
        assertFalse(shouldShow(eligible, deferredThisLaunch = true))
        assertFalse(shouldShow(eligible, isHome = false))
        assertFalse(shouldShow(eligible, isForeground = false))
        assertFalse(shouldShow(eligible, startupPromptChecked = false))
        assertFalse(shouldShow(eligible, hasBlockingPrompt = true))
    }

    @Test
    fun `only an actual permission prompt defers the monthly notice`() {
        assertFalse(
            shouldDeferMonthlyPriceNoticeForNotificationPrompt(
                NotificationPermissionPromptStatus.PENDING_CHECK,
            ),
        )
        assertTrue(
            shouldDeferMonthlyPriceNoticeForNotificationPrompt(
                NotificationPermissionPromptStatus.PROMPT_REQUIRED_OR_IN_PROGRESS,
            ),
        )
        assertFalse(
            shouldDeferMonthlyPriceNoticeForNotificationPrompt(
                NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED,
            ),
        )
        assertFalse(
            shouldShow(
                MonthlyPriceNoticeEligibility.Eligible("4,89 zł", "14,99 zł"),
                notificationPermissionPromptStatus = NotificationPermissionPromptStatus.PENDING_CHECK,
            ),
        )
        assertFalse(
            shouldShow(
                MonthlyPriceNoticeEligibility.Eligible("4,89 zł", "14,99 zł"),
                notificationPermissionPromptStatus =
                    NotificationPermissionPromptStatus.PROMPT_REQUIRED_OR_IN_PROGRESS,
            ),
        )
        assertTrue(
            shouldShow(
                MonthlyPriceNoticeEligibility.Eligible("4,89 zł", "14,99 zł"),
                notificationPermissionPromptStatus = NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED,
            ),
        )
    }

    private fun shouldShow(
        eligibility: MonthlyPriceNoticeEligibility,
        acknowledged: Boolean = false,
        dismissedThisLaunch: Boolean = false,
        deferredThisLaunch: Boolean = false,
        isHome: Boolean = true,
        isForeground: Boolean = true,
        startupPromptChecked: Boolean = true,
        hasBlockingPrompt: Boolean = false,
        notificationPermissionPromptStatus: NotificationPermissionPromptStatus =
            NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED,
    ) = shouldShowMonthlyPriceNotice(
        eligibility = eligibility,
        acknowledged = acknowledged,
        dismissedThisLaunch = dismissedThisLaunch,
        deferredThisLaunch = deferredThisLaunch,
        isHome = isHome,
        isForeground = isForeground,
        startupPromptChecked = startupPromptChecked,
        hasBlockingPrompt = hasBlockingPrompt,
        notificationPermissionPromptStatus = notificationPermissionPromptStatus,
    )
}

private class MonthlyPriceNoticeTestPreferenceStore : PreferenceStore {
    private val values = mutableMapOf<String, Boolean>()

    override fun contains(key: String) = key in values
    override fun getString(key: String): String? = null
    override fun putString(key: String, value: String) = Unit
    override fun getBoolean(key: String) = values[key] ?: false
    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }
}
