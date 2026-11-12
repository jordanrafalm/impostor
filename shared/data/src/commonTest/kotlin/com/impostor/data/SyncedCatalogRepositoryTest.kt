package com.impostor.data

import com.impostor.domain.CatalogRepository
import com.impostor.domain.Category
import com.impostor.domain.Prompt
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

internal class SyncedCatalogRepositoryTest {
    private val bundled = object : CatalogRepository {
        override fun categories() = listOf(Category("animals", "Zwierzęta", "paw"))
        override fun prompts(categoryIds: Set<String>) =
            listOf(Prompt("animals-001", "animals", "Kot", "Mleko")).filter { it.categoryId in categoryIds }
    }

    private class FakeSource(var result: () -> List<RemotePrompt>) : RemotePromptSource {
        override suspend fun fetchPrompts() = result()
    }

    private class FakeStore(var value: String? = null, var failWrite: Boolean = false) : PromptSnapshotStore {
        override fun read() = value
        override fun write(snapshot: String) {
            if (failWrite) error("disk full")
            value = snapshot
        }
    }

    private fun remote(
        id: String,
        category: String? = "animals",
        text: String? = "Wydra",
        hint: String? = "Rzeka",
        active: Boolean = true,
        locale: String? = null,
    ) = RemotePrompt(id, category, text, hint, active, locale)

    private fun ids(repository: CatalogRepository) = repository.prompts(setOf("animals")).map { it.id }

    @Test
    fun `should merge valid remote prompts after bundled prompts`() = runTest {
        val repository = SyncedCatalogRepository(bundled, FakeSource { listOf(remote("r2"), remote("r1")) }, FakeStore())

        repository.refresh()

        assertEquals(listOf("animals-001", "r1", "r2"), ids(repository))
    }

    @Test
    fun `should keep bundled gameplay available when Firebase is not configured`() = runTest {
        val source = FirestorePromptSource { error("Firebase is not configured") }
        val repository = SyncedCatalogRepository(bundled, source, FakeStore())

        assertEquals(listOf("animals-001"), ids(repository))
        assertTrue(repository.refresh().isFailure)
        assertEquals(listOf("animals-001"), ids(repository))
    }

    @Test
    fun `should not duplicate ids across repeated syncs or with bundled prompts`() = runTest {
        val source = FakeSource { listOf(remote("animals-001", text = "Inny"), remote("r1"), remote("r1")) }
        val repository = SyncedCatalogRepository(bundled, source, FakeStore())

        repository.refresh()
        repository.refresh()

        assertEquals(listOf("animals-001", "r1"), ids(repository))
        assertEquals("Kot", repository.prompts(setOf("animals")).first().text)
    }

    @Test
    fun `should replace cached same-locale remote prompt with the latest valid record`() = runTest {
        val store = FakeStore()
        var records = listOf(remote("r1", text = "Stary tekst", hint = "Stara podpowiedź"))
        val source = FakeSource { records }
        val repository = SyncedCatalogRepository(bundled, source, store)
        repository.refresh()

        records = listOf(remote("r1", text = "Nowszy tekst", hint = "Nowa podpowiedź"))
        assertTrue(repository.refresh().isSuccess)

        assertEquals("Nowszy tekst", repository.prompts(setOf("animals")).last().text)
        val restarted = SyncedCatalogRepository(bundled, FakeSource { error("offline") }, store)
        assertEquals("Nowszy tekst", restarted.prompts(setOf("animals")).last().text)
    }

    @Test
    fun `should ignore inactive and invalid remote records`() = runTest {
        val source = FakeSource {
            listOf(
                remote("ok"), remote("off", active = false), remote("cat", category = "unknown"),
                remote("nocat", category = null), remote("blank", text = " "), remote("nohint", hint = null),
            )
        }
        val repository = SyncedCatalogRepository(bundled, source, FakeStore())

        repository.refresh()

        assertEquals(listOf("animals-001", "ok"), ids(repository))
    }

    @Test
    fun `should reuse persisted snapshot after restart without network`() = runTest {
        val store = FakeStore()
        SyncedCatalogRepository(bundled, FakeSource { listOf(remote("r1")) }, store).refresh()

        val restarted = SyncedCatalogRepository(bundled, FakeSource { error("offline") }, store)

        assertEquals(listOf("animals-001", "r1"), ids(restarted))
        restarted.refresh()
        assertEquals(listOf("animals-001", "r1"), ids(restarted))
    }

    @Test
    fun `should keep last valid snapshot when fetch fails or returns only invalid data`() = runTest {
        val store = FakeStore()
        val source = FakeSource { listOf(remote("r1")) }
        val repository = SyncedCatalogRepository(bundled, source, store)
        assertTrue(repository.refresh().isSuccess)
        val saved = store.value

        source.result = { error("offline") }
        assertTrue(repository.refresh().isFailure)
        source.result = { listOf(remote("bad", text = "")) }
        assertTrue(repository.refresh().isSuccess)

        assertEquals(listOf("animals-001", "r1"), ids(repository))
        assertEquals(saved, store.value)
    }

    @Test
    fun `should keep cached prompts when later response omits or deactivates them`() = runTest {
        val store = FakeStore()
        val source = FakeSource { listOf(remote("r1"), remote("r2")) }
        val repository = SyncedCatalogRepository(bundled, source, store)
        repository.refresh()

        source.result = { listOf(remote("r1", active = false), remote("r3")) }
        repository.refresh()

        val expected = listOf("animals-001", "r1", "r2", "r3")
        assertEquals(expected, ids(repository))
        assertEquals(expected, ids(SyncedCatalogRepository(bundled, FakeSource { error("offline") }, store)))
    }

    @Test
    fun `should fall back to bundled prompts when cache is corrupt or write fails`() = runTest {
        val corrupt = SyncedCatalogRepository(bundled, FakeSource { emptyList() }, FakeStore("{not json"))
        assertEquals(listOf("animals-001"), ids(corrupt))

        val failing = SyncedCatalogRepository(bundled, FakeSource { listOf(remote("r1")) }, FakeStore(failWrite = true))
        assertTrue(failing.refresh().isFailure)
        assertEquals(listOf("animals-001"), ids(failing))
    }

    @Test
    fun `should not change prompt lists already handed out`() = runTest {
        val repository = SyncedCatalogRepository(bundled, FakeSource { listOf(remote("r1")) }, FakeStore())
        val before = repository.prompts(setOf("animals"))

        repository.refresh()

        assertEquals(listOf("animals-001"), before.map { it.id })
    }

    @Test
    fun `should propagate cancellation from remote source`() = runTest {
        val repository = SyncedCatalogRepository(bundled, FakeSource { throw CancellationException("stop") }, FakeStore())

        assertFailsWith<CancellationException> { repository.refresh() }
        assertTrue(ids(repository) == listOf("animals-001"))
    }

    @Test
    fun `should keep untagged and Polish remote prompts out of the English catalog`() = runTest {
        // Given
        val store = FakeStore()
        val source = FakeSource {
            listOf(
                remote("untagged"),
                remote("polish", text = "Wydra", locale = "pl-PL"),
                remote("english", text = "Otter", hint = "River", locale = "en-GB"),
            )
        }
        val repository = SyncedCatalogRepository(bundled, source, store, AppLocale.ENGLISH)

        // When
        repository.refresh()

        // Then
        assertEquals(listOf("animals-001", "english"), ids(repository))
        assertEquals("Otter", repository.prompts(setOf("animals")).last().text)
    }

    @Test
    fun `should preserve remote prompts for each locale in the shared snapshot`() = runTest {
        // Given
        val store = FakeStore()
        val source = FakeSource {
            listOf(
                remote("localized", text = "Wydra", locale = "pl"),
                remote("localized", text = "Otter", hint = "River", locale = "en"),
            )
        }

        // When
        SyncedCatalogRepository(bundled, source, store, AppLocale.ENGLISH).refresh()
        val polishRestart = SyncedCatalogRepository(
            bundled,
            FakeSource { error("offline") },
            store,
            AppLocale.POLISH,
        )

        // Then
        assertEquals(listOf("animals-001", "localized"), ids(polishRestart))
        assertEquals("Wydra", polishRestart.prompts(setOf("animals")).last().text)
    }

    @Test
    fun `should preserve the Polish default for remote records without a locale`() = runTest {
        // Given
        val repository = SyncedCatalogRepository(
            bundled,
            FakeSource { listOf(remote("untagged")) },
            FakeStore(),
            AppLocale.POLISH,
        )

        // When
        repository.refresh()

        // Then
        assertEquals(listOf("animals-001", "untagged"), ids(repository))
    }

    @Test
    fun `should read legacy snapshots as Polish records`() = runTest {
        // Given
        val legacySnapshot = """
            {
              "schemaVersion": 1,
              "prompts": [
                {"id": "legacy", "categoryId": "animals", "text": "Wydra", "hint": "Rzeka"}
              ]
            }
        """.trimIndent()

        // When
        val polishRepository = SyncedCatalogRepository(
            bundled,
            FakeSource { error("offline") },
            FakeStore(legacySnapshot),
            AppLocale.POLISH,
        )
        val englishRepository = SyncedCatalogRepository(
            bundled,
            FakeSource { error("offline") },
            FakeStore(legacySnapshot),
            AppLocale.ENGLISH,
        )

        // Then
        assertEquals(listOf("animals-001", "legacy"), ids(polishRepository))
        assertEquals(listOf("animals-001"), ids(englishRepository))
    }
}
