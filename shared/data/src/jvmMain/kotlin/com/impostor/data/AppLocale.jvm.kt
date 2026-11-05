package com.impostor.data

import java.util.Locale

internal actual fun platformPreferredLanguageTags(): List<String> =
    listOf(Locale.getDefault().toLanguageTag())
