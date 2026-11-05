package com.impostor.data

import android.os.LocaleList

internal actual fun platformPreferredLanguageTags(): List<String> =
    (0 until LocaleList.getDefault().size()).map { index ->
        LocaleList.getDefault()[index].toLanguageTag()
    }
