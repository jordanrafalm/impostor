package com.impostor.data

import android.content.Context

private class SharedPreferencesPromptSnapshotStore(context: Context) : PromptSnapshotStore {
    private val preferences = context.getSharedPreferences("prompt_catalog", Context.MODE_PRIVATE)

    override fun read(): String? = preferences.getString(KEY, null)

    override fun write(snapshot: String) {
        check(preferences.edit().putString(KEY, snapshot).commit()) { "Unable to persist prompt snapshot" }
    }

    private companion object {
        const val KEY = "remote_snapshot"
    }
}

actual fun platformPromptSnapshotStore(): PromptSnapshotStore =
    catalogContext?.let { SharedPreferencesPromptSnapshotStore(it) } ?: InMemoryPromptSnapshotStore()
