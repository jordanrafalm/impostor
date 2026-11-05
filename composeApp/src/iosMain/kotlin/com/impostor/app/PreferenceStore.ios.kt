package com.impostor.app

import platform.Foundation.NSUserDefaults

private object IosPreferenceStore : PreferenceStore {
    private val defaults get() = NSUserDefaults.standardUserDefaults

    override fun contains(key: String) = defaults.objectForKey(key) != null
    override fun getString(key: String): String? = defaults.stringForKey(key)
    override fun putString(key: String, value: String) = defaults.setObject(value, forKey = key)
    override fun getBoolean(key: String) = defaults.boolForKey(key)
    override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, forKey = key)
}

internal actual fun platformPreferenceStore(): PreferenceStore = IosPreferenceStore
