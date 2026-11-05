package com.impostor.data

import platform.Foundation.NSUserDefaults

internal actual fun platformPreferredLanguageTags(): List<String> =
    (NSUserDefaults.standardUserDefaults.arrayForKey("AppleLanguages") as? List<*>)
        ?.mapNotNull { it as? String }
        .orEmpty()
