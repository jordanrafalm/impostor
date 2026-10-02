package com.impostor.data

import com.impostor.domain.CatalogRepository
import com.impostor.domain.Category
import com.impostor.domain.Prompt
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Unvalidated prompt record as published remotely. */
data class RemotePrompt(
    val id: String,
    val categoryId: String?,
    val text: String?,
    val hint: String?,
    val active: Boolean,
)

interface RemotePromptSource {
    /** Returns all remote prompt records; throws on any fetch failure. */
    suspend fun fetchPrompts(): List<RemotePrompt>
}

interface PromptSnapshotStore {
    fun read(): String?
    fun write(snapshot: String)
}

@Serializable
private data class PromptSnapshot(
    val schemaVersion: Int = SNAPSHOT_VERSION,
    val prompts: List<SnapshotPrompt>,
)

@Serializable
private data class SnapshotPrompt(val id: String, val categoryId: String, val text: String, val hint: String)

private const val SNAPSHOT_VERSION = 1
private const val MAX_FIELD_LENGTH = 200
private val snapshotJson = Json { ignoreUnknownKeys = true }

/**
 * Bundled catalog plus additive remote prompts. Reads always see an immutable snapshot, so a
 * refresh never alters a list already handed out (e.g. to a game in progress).
 */
class SyncedCatalogRepository(
    private val bundled: CatalogRepository,
    private val source: RemotePromptSource,
    private val store: PromptSnapshotStore,
) : CatalogRepository {
    private val categories = bundled.categories()
    private val categoryIds = categories.map { it.id }.toSet()
    private val bundledPrompts = bundled.prompts(categoryIds)
    private val bundledIds = bundledPrompts.mapTo(mutableSetOf()) { it.id }

    @Volatile
    private var remotePrompts: List<Prompt> = loadCachedPrompts()

    override fun categories(): List<Category> = categories

    override fun prompts(categoryIds: Set<String>): List<Prompt> =
        mergePrompts(bundledPrompts, remotePrompts).filter { it.categoryId in categoryIds }

    /** Refreshes from the remote source; failures keep the previous valid data and are silent. */
    suspend fun refresh() {
        val records = try {
            source.fetchPrompts()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return
        }
        val valid = validatePrompts(records, categoryIds)
        if (valid.isEmpty()) return
        val merged = mergePrompts(remotePrompts, valid).filter { it.id !in bundledIds }
        if (merged == remotePrompts) return
        val persisted = try {
            store.write(snapshotJson.encodeToString(PromptSnapshot(prompts = merged.map {
                SnapshotPrompt(it.id, it.categoryId, it.text, it.hint)
            })))
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }
        // Keep the in-memory view consistent with what a restart would load only when persisted.
        if (persisted) remotePrompts = merged
    }

    private fun loadCachedPrompts(): List<Prompt> = try {
        store.read()?.let { raw ->
            val snapshot = snapshotJson.decodeFromString<PromptSnapshot>(raw)
            if (snapshot.schemaVersion != SNAPSHOT_VERSION) return emptyList()
            validatePrompts(
                snapshot.prompts.map { RemotePrompt(it.id, it.categoryId, it.text, it.hint, active = true) },
                categoryIds,
            )
        }.orEmpty()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun validatePrompts(records: List<RemotePrompt>, knownCategoryIds: Set<String>): List<Prompt> =
    records.asSequence()
        .filter { it.active }
        .mapNotNull { record ->
            val id = record.id.trim()
            val text = record.text?.trim().orEmpty()
            val hint = record.hint?.trim().orEmpty()
            val categoryId = record.categoryId?.trim().orEmpty()
            if (id.isEmpty() || id.length > MAX_FIELD_LENGTH || categoryId !in knownCategoryIds ||
                text.isEmpty() || text.length > MAX_FIELD_LENGTH ||
                hint.isEmpty() || hint.length > MAX_FIELD_LENGTH
            ) null else Prompt(id, categoryId, text, hint)
        }
        .distinctBy { it.id }
        .sortedBy { it.id }
        .toList()

internal fun mergePrompts(bundled: List<Prompt>, remote: List<Prompt>): List<Prompt> {
    val seen = bundled.mapTo(mutableSetOf()) { it.id }
    return bundled + remote.filter { seen.add(it.id) }
}

class InMemoryPromptSnapshotStore : PromptSnapshotStore {
    private var snapshot: String? = null
    override fun read(): String? = snapshot
    override fun write(snapshot: String) {
        this.snapshot = snapshot
    }
}
