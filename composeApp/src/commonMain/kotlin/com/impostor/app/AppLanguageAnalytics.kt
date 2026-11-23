package com.impostor.app

import com.impostor.domain.AnalyticsService

internal fun reportAppLanguage(
    language: String,
    store: PreferenceStore,
    analytics: AnalyticsService,
) {
    if (language !in setOf("en", "pl", "uk", "ar", "el", "es", "it", "pt", "ro")) {
        println("Cannot log unsupported app language: $language")
        return
    }
    if (store.getString(PreferenceKeys.LAST_REPORTED_APP_LANGUAGE) == language) return
    if (analytics.logAppLanguage(language)) {
        store.putString(PreferenceKeys.LAST_REPORTED_APP_LANGUAGE, language)
    }
}
