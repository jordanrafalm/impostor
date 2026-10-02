package com.impostor.app

import platform.Foundation.NSBundle

actual fun appVersion(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        ?: error("CFBundleShortVersionString is missing from the app bundle")
