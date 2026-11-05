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
    val locale: String? = null,
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
private data class SnapshotPrompt(
    val id: String,
    val categoryId: String,
    val text: String,
    val hint: String,
    val locale: String = AppLocale.POLISH.languageTag,
)

private const val SNAPSHOT_VERSION = 2
private const val PREVIOUS_SNAPSHOT_VERSION = 1
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
    private val locale: AppLocale = currentAppLocale(),
) : CatalogRepository {
    private val categories = bundled.categories()
    private val categoryIds = categories.map { it.id }.toSet()
    private val bundledPrompts = bundled.prompts(categoryIds)
    private val bundledIds = bundledPrompts.mapTo(mutableSetOf()) { it.id }

    @Volatile
    private var cachedRemotePrompts: List<RemotePrompt> = loadCachedPrompts()

    @Volatile
    private var remotePrompts: List<Prompt> = validatePrompts(cachedRemotePrompts, categoryIds, locale)

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
        val valid = validateRemotePrompts(records, categoryIds)
        if (valid.isEmpty()) return
        val merged = mergeRemotePrompts(cachedRemotePrompts, valid)
            .filter { it.id !in bundledIds }
        if (merged == cachedRemotePrompts) return
        val persisted = try {
            store.write(snapshotJson.encodeToString(PromptSnapshot(prompts = merged.map {
                SnapshotPrompt(it.id, it.categoryId.orEmpty(), it.text.orEmpty(), it.hint.orEmpty(), it.locale ?: "pl")
            })))
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }
        // Keep the in-memory view consistent with what a restart would load only when persisted.
        if (persisted) {
            cachedRemotePrompts = merged
            remotePrompts = validatePrompts(cachedRemotePrompts, categoryIds, locale)
        }
    }

    private fun loadCachedPrompts(): List<RemotePrompt> = try {
        store.read()?.let { raw ->
            val snapshot = snapshotJson.decodeFromString<PromptSnapshot>(raw)
            if (snapshot.schemaVersion != SNAPSHOT_VERSION &&
                snapshot.schemaVersion != PREVIOUS_SNAPSHOT_VERSION
            ) {
                return emptyList()
            }
            validateRemotePrompts(snapshot.prompts.map {
                RemotePrompt(it.id, it.categoryId, it.text, it.hint, active = true, locale = it.locale)
            }, categoryIds)
        }.orEmpty()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        emptyList()
    }
}

private fun validateRemotePrompts(
    records: List<RemotePrompt>,
    knownCategoryIds: Set<String>,
): List<RemotePrompt> =
    records.asSequence()
        .filter { it.active }
        .mapNotNull { record ->
            val id = record.id.trim()
            val text = record.text?.trim().orEmpty()
            val hint = record.hint?.trim().orEmpty()
            val categoryId = record.categoryId?.trim().orEmpty()
            val locale = normalizedRemoteLocale(record.locale)
            if (id.isEmpty() || id.length > MAX_FIELD_LENGTH || categoryId !in knownCategoryIds ||
                text.isEmpty() || text.length > MAX_FIELD_LENGTH ||
                hint.isEmpty() || hint.length > MAX_FIELD_LENGTH || locale == null
            ) {
                null
            } else {
                record.copy(
                    id = id,
                    categoryId = categoryId,
                    text = text,
                    hint = hint,
                    locale = locale,
                )
            }
        }
        .distinctBy { it.locale to it.id }
        .sortedWith(compareBy<RemotePrompt> { it.locale }.thenBy { it.id })
        .toList()

private fun normalizedRemoteLocale(locale: String?): String? {
    val language = locale?.trim()?.substringBefore('-')?.substringBefore('_')?.lowercase()
        ?: AppLocale.POLISH.languageTag
    return language.takeIf { it == AppLocale.POLISH.languageTag || it == AppLocale.ENGLISH.languageTag }
}

private fun validatePrompts(
    records: List<RemotePrompt>,
    knownCategoryIds: Set<String>,
    locale: AppLocale,
): List<Prompt> = validateRemotePrompts(records, knownCategoryIds)
    .filter { normalizedRemoteLocale(it.locale) == locale.languageTag }
    .map { Prompt(it.id, it.categoryId.orEmpty(), it.text.orEmpty(), it.hint.orEmpty()) }

private fun mergeRemotePrompts(
    previous: List<RemotePrompt>,
    incoming: List<RemotePrompt>,
): List<RemotePrompt> {
    val merged = linkedMapOf<Pair<String, String>, RemotePrompt>()
    (previous + incoming).forEach { record ->
        val locale = normalizedRemoteLocale(record.locale) ?: return@forEach
        val key = locale to record.id
        if (key !in merged) merged[key] = record
    }
    return merged.values.toList()
}

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
