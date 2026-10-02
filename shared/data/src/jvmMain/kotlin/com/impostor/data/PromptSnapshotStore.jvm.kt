package com.impostor.data

actual fun platformPromptSnapshotStore(): PromptSnapshotStore = InMemoryPromptSnapshotStore()
