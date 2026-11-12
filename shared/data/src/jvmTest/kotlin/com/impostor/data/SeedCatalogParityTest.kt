package com.impostor.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SeedCatalogParityTest {
    @Test
    fun `Christmas category contains fifty distinct prompts with explicit hints in every locale`() {
        val catalogs = listOf("pl", "en", "uk").map { locale ->
            val json = resource("/seed/prompts-$locale.json")
            val document = Json.parseToJsonElement(json).jsonObject
            assertTrue(document.getValue("activeCategoryIds").jsonArray.any { it.jsonPrimitive.content == "christmas" })
            val category = document.getValue("categories").jsonArray
                .map(JsonElement::jsonObject).single { it.text("id") == "christmas" }
            val entries = category.getValue("prompts").jsonArray
            assertEquals(50, entries.size, locale)
            assertEquals(50, entries.map { it.jsonObject.text("text").lowercase() }.toSet().size, locale)
            entries.forEach {
                val entry = it.jsonObject
                assertTrue(entry.text("text").isNotBlank(), locale)
                assertTrue(entry.text("hint").isNotBlank(), locale)
                assertTrue(!entry.text("text").equals(entry.text("hint"), ignoreCase = true), locale)
            }
            SeedCatalogRepository(json).prompts(setOf("christmas"))
        }
        assertTrue(catalogs.all { it.size == 50 })
        assertEquals(catalogs[0].map { it.id }, catalogs[1].map { it.id })
        assertEquals(catalogs[0].map { it.id }, catalogs[2].map { it.id })
    }

    @Test
    fun `English seed preserves catalog structure and has complete translated content`() {
        val polishJson = resource("/seed/prompts-pl.json")
        val englishJson = resource("/seed/prompts-en.json")
        val polish = Json.parseToJsonElement(polishJson).jsonObject
        val english = Json.parseToJsonElement(englishJson).jsonObject
        val polishCategories = polish.getValue("categories").jsonArray.map(JsonElement::jsonObject)
        val englishCategories = english.getValue("categories").jsonArray.map(JsonElement::jsonObject)

        assertEquals(polish.getValue("schemaVersion"), english.getValue("schemaVersion"))
        assertEquals(polish.getValue("activeCategoryIds"), english.getValue("activeCategoryIds"))
        assertEquals(
            polishCategories.map { it.text("id") },
            englishCategories.map { it.text("id") },
            "Category IDs and order must remain stable",
        )

        for ((polishCategory, englishCategory) in polishCategories.zip(englishCategories)) {
            val categoryId = englishCategory.text("id")
            assertTrue(englishCategory.text("name").isNotBlank(), "$categoryId has a blank name")
            if (categoryId !in setOf("adult", "sex")) {
                assertTrue(
                    polishCategory.text("name") != englishCategory.text("name"),
                    "$categoryId category name was not translated",
                )
            }
            val polishPrompts = polishCategory.getValue("prompts").jsonArray
            val englishPrompts = englishCategory.getValue("prompts").jsonArray
            assertEquals(
                polishPrompts.size,
                englishPrompts.size,
                "$categoryId prompt count must remain stable",
            )
            assertEnglishPromptsHaveTextAndHints(categoryId, englishPrompts)
        }

        val activeIds = english.getValue("activeCategoryIds").jsonArray
            .map { it.jsonPrimitive.content }
            .toSet()
        val polishRepository = SeedCatalogRepository(polishJson)
        val englishRepository = SeedCatalogRepository(englishJson)
        assertEquals(
            polishRepository.prompts(activeIds).map { it.id },
            englishRepository.prompts(activeIds).map { it.id },
            "Prompt IDs used by saved game state must remain stable",
        )
        val activeEnglishPrompts = englishRepository.prompts(activeIds)
        assertTrue(activeEnglishPrompts.all { it.text.isNotBlank() && it.hint.isNotBlank() })
        assertEquals(
            englishCategories.filter { it.text("id") in activeIds }.sumOf {
                it.getValue("prompts").jsonArray.size
            },
            activeEnglishPrompts.size,
        )

        assertEquals("Animals", englishCategories.first().text("name"))
        assertEquals("Penguin", activeEnglishPrompts.first().text)
        assertEquals("Antarctica", activeEnglishPrompts.first().hint)
        assertEquals("Departures", englishRepository.prompts(setOf("places")).first().hint)
    }

    @Test
    fun `Ukrainian seed preserves catalog structure and prompt IDs`() {
        val polishJson = resource("/seed/prompts-pl.json")
        val ukrainianJson = resource("/seed/prompts-uk.json")
        val polish = Json.parseToJsonElement(polishJson).jsonObject
        val ukrainian = Json.parseToJsonElement(ukrainianJson).jsonObject
        val polishCategories = polish.getValue("categories").jsonArray.map(JsonElement::jsonObject)
        val ukrainianCategories = ukrainian.getValue("categories").jsonArray.map(JsonElement::jsonObject)

        assertEquals(polish.getValue("schemaVersion"), ukrainian.getValue("schemaVersion"))
        assertEquals(polish.getValue("activeCategoryIds"), ukrainian.getValue("activeCategoryIds"))
        assertEquals(polishCategories.map { it.text("id") }, ukrainianCategories.map { it.text("id") })
        for ((polishCategory, ukrainianCategory) in polishCategories.zip(ukrainianCategories)) {
            val categoryId = ukrainianCategory.text("id")
            assertTrue(ukrainianCategory.text("name").isNotBlank(), "$categoryId has a blank name")
            val prompts = ukrainianCategory.getValue("prompts").jsonArray
            assertEquals(polishCategory.getValue("prompts").jsonArray.size, prompts.size, "$categoryId prompt count")
            assertEnglishPromptsHaveTextAndHints(categoryId, prompts)
        }
        val activeIds = ukrainian.getValue("activeCategoryIds").jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertEquals(
            SeedCatalogRepository(polishJson).prompts(activeIds).map { it.id },
            SeedCatalogRepository(ukrainianJson).prompts(activeIds).map { it.id },
        )
    }

    private fun assertEnglishPromptsHaveTextAndHints(categoryId: String, prompts: JsonArray) {
        val texts = prompts.map { it.promptText() }
        prompts.forEachIndexed { index, prompt ->
            assertTrue(
                prompt is JsonObject,
                "$categoryId prompt ${index + 1} must include its translated English clue",
            )
            val text = texts[index]
            val hint = prompt.jsonObjectOrNull()?.get("hint")?.jsonPrimitive?.content
                ?: texts[(index + 1) % texts.size]
            assertTrue(text.isNotBlank(), "$categoryId prompt ${index + 1} has blank text")
            assertTrue(hint.isNotBlank(), "$categoryId prompt ${index + 1} has blank hint")
        }
    }

    private fun JsonElement.promptText(): String =
        jsonObjectOrNull()?.get("text")?.jsonPrimitive?.content ?: jsonPrimitive.content

    private fun JsonElement.jsonObjectOrNull(): JsonObject? =
        (this as? JsonObject)

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

    private fun resource(path: String): String =
        requireNotNull(javaClass.getResourceAsStream(path)) { "Missing catalog resource: $path" }
            .bufferedReader()
            .use { it.readText() }
}
