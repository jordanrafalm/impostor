package com.impostor.app

import com.impostor.domain.AnalyticsEvent
import com.impostor.domain.AnalyticsService
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AppLanguageAnalyticsTest {
    @Test
    fun `reports once per language change and persists across instances`() {
        val store = LanguageTestStore()
        val analytics = LanguageTestAnalytics()

        reportAppLanguage("pl", store, analytics)
        reportAppLanguage("pl", store, analytics)
        reportAppLanguage("en", store, analytics)
        reportAppLanguage("pl", store, analytics)

        assertEquals(listOf("pl", "en", "pl"), analytics.languages)
    }

    @Test
    fun `retries a failed report without saving it`() {
        val store = LanguageTestStore()
        val analytics = LanguageTestAnalytics(succeeds = false)

        reportAppLanguage("uk", store, analytics)
        analytics.succeeds = true
        reportAppLanguage("uk", store, analytics)
        reportAppLanguage("uk", store, analytics)

        assertEquals(listOf("uk", "uk"), analytics.languages)
        assertEquals("uk", store.getString(PreferenceKeys.LAST_REPORTED_APP_LANGUAGE))
    }

    @Test
    fun `does not report an invalid language or prevent a later valid report`() {
        val store = LanguageTestStore()
        val analytics = LanguageTestAnalytics()

        reportAppLanguage("", store, analytics)
        reportAppLanguage("en", store, analytics)

        assertEquals(listOf("en"), analytics.languages)
    }

    @Test
    fun `reports every supported new locale`() {
        val store = LanguageTestStore()
        val analytics = LanguageTestAnalytics()

        listOf("ar", "el", "es", "it", "pt", "ro").forEach { language ->
            reportAppLanguage(language, store, analytics)
        }

        assertEquals(listOf("ar", "el", "es", "it", "pt", "ro"), analytics.languages)
    }

    @Test
    fun `reports every supported language once and rejects unsupported tags`() {
        val store = LanguageTestStore()
        val analytics = LanguageTestAnalytics()
        val languages = listOf("en", "pl", "uk", "ar", "el", "es", "it", "pt", "ro")

        languages.forEach { language ->
            reportAppLanguage(language, store, analytics)
            reportAppLanguage(language, store, analytics)
        }
        listOf("", "fr", "pt-BR", "AR").forEach { reportAppLanguage(it, store, analytics) }

        assertEquals(languages, analytics.languages)
        assertEquals("ro", store.getString(PreferenceKeys.LAST_REPORTED_APP_LANGUAGE))
    }
}

private class LanguageTestAnalytics(var succeeds: Boolean = true) : AnalyticsService {
    val languages = mutableListOf<String>()

    override fun log(event: AnalyticsEvent, parameters: Map<String, String>) = Unit
    override fun logAppLanguage(language: String): Boolean {
        languages += language
        return succeeds
    }
}

private class LanguageTestStore : PreferenceStore {
    private val values = mutableMapOf<String, String>()

    override fun contains(key: String) = key in values
    override fun getString(key: String) = values[key]
    override fun putString(key: String, value: String) { values[key] = value }
    override fun getBoolean(key: String) = false
    override fun putBoolean(key: String, value: Boolean) = Unit
}
