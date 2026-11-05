package com.impostor.data

/** Languages supported by the bundled catalog and shared UI. */
enum class AppLocale(val languageTag: String) {
    ENGLISH("en"),
    POLISH("pl"),
}

/**
 * Resolves the first supported language in the device's preferred language list.
 * Unsupported-only lists use English as the fallback.
 */
fun resolveAppLocale(preferredTags: List<String>): AppLocale =
    preferredTags.firstNotNullOfOrNull { tag ->
        when (tag.substringBefore('-').substringBefore('_').lowercase()) {
            "pl" -> AppLocale.POLISH
            "en" -> AppLocale.ENGLISH
            else -> null
        }
    } ?: AppLocale.ENGLISH

internal fun currentAppLocale(): AppLocale = resolveAppLocale(platformPreferredLanguageTags())

internal expect fun platformPreferredLanguageTags(): List<String>

internal val AppLocale.catalogFileName: String
    get() = "prompts-$languageTag.json"
