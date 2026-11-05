package com.impostor.app

import android.content.Context
import android.content.SharedPreferences

private const val PREFERENCES_NAME = "impostor_preferences"

private var applicationContext: Context? = null

internal fun initializePreferenceStore(context: Context) {
    applicationContext = context.applicationContext
}

private class AndroidPreferenceStore(private val preferences: SharedPreferences) : PreferenceStore {
    override fun contains(key: String) = preferences.contains(key)
    override fun getString(key: String): String? = preferences.getString(key, null)
    override fun putString(key: String, value: String) = preferences.edit().putString(key, value).apply()
    override fun getBoolean(key: String) = preferences.getBoolean(key, false)
    override fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()
}

internal actual fun platformPreferenceStore(): PreferenceStore {
    val context = requireNotNull(applicationContext) { "Preference store must be initialized before use" }
    return AndroidPreferenceStore(context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE))
}
