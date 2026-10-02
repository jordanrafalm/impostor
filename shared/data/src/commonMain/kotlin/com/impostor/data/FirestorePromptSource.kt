package com.impostor.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

@Serializable
internal data class PromptDocument(
    val categoryId: String? = null,
    val text: String? = null,
    val hint: String? = null,
    val active: Boolean = false,
)

internal class FirestorePromptSource(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : RemotePromptSource {
    override suspend fun fetchPrompts(): List<RemotePrompt> =
        firestore.collection("prompts").get().documents.mapNotNull { document ->
            if (!document.exists) return@mapNotNull null
            // A malformed document is skipped without discarding the rest of the collection.
            val data = runCatching { document.data<PromptDocument>() }.getOrNull() ?: return@mapNotNull null
            RemotePrompt(document.id, data.categoryId, data.text, data.hint, data.active)
        }
}

expect fun platformPromptSnapshotStore(): PromptSnapshotStore

fun syncedCatalogRepository(): SyncedCatalogRepository = SyncedCatalogRepository(
    bundledCatalogRepository(),
    FirestorePromptSource(),
    platformPromptSnapshotStore(),
)
