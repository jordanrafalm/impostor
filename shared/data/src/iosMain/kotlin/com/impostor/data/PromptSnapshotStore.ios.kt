package com.impostor.data

import platform.Foundation.NSUserDefaults

private class UserDefaultsPromptSnapshotStore : PromptSnapshotStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(): String? = defaults.stringForKey(KEY)

    override fun write(snapshot: String) {
        defaults.setObject(snapshot, forKey = KEY)
    }

    private companion object {
        const val KEY = "prompt_catalog_remote_snapshot"
    }
}

actual fun platformPromptSnapshotStore(): PromptSnapshotStore = UserDefaultsPromptSnapshotStore()
