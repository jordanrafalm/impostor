package com.impostor.data

import com.impostor.domain.CatalogRepository
import com.impostor.domain.Category
import com.impostor.domain.Prompt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class SeedDocument(
    val schemaVersion: Int,
    val activeCategoryIds: List<String>,
    val categories: List<SeedCategory>,
)

@Serializable
private data class SeedCategory(
    val id: String,
    val name: String,
    val iconKey: String,
    val prompts: List<JsonElement>,
)

class SeedCatalogRepository(seedJson: String) : CatalogRepository {
    private val seed = seedJsonFormat.decodeFromString<SeedDocument>(seedJson)
    private val activeCategories = seed.activeCategoryIds.mapNotNull { id ->
        seed.categories.firstOrNull { it.id == id }
    }
    private val promptHints = buildPromptHints(activeCategories)

    override fun categories(): List<Category> = activeCategories.map {
        Category(it.id, it.name, it.iconKey)
    }

    override fun prompts(categoryIds: Set<String>): List<Prompt> = activeCategories
        .filter { it.id in categoryIds }
        .flatMap { category ->
            category.prompts.mapIndexed { index, prompt ->
                val (id, text) = prompt.toSeedPrompt(category.id, index)
                Prompt(id, category.id, text, prompt.hint(text, promptHints))
            }
        }
}

private fun JsonElement.toSeedPrompt(categoryId: String, index: Int): Pair<String, String> =
    when (this) {
        is JsonPrimitive -> "$categoryId-${(index + 1).toString().padStart(3, '0')}" to jsonPrimitive.content
        else -> (jsonObject["id"]?.jsonPrimitive?.content
            ?: "$categoryId-${(index + 1).toString().padStart(3, '0')}") to
            jsonObject["text"]!!.jsonPrimitive.content
    }

private fun JsonElement.hint(text: String, promptHints: Map<String, String>): String =
    if (this is JsonPrimitive) {
        promptHints[text] ?: error("Missing semantic hint for seed prompt: $text")
    } else {
        jsonObject["hint"]?.jsonPrimitive?.content
            ?.takeIf { it.isNotBlank() }
            ?: promptHints[text]
            ?: error("Missing semantic hint for seed prompt: $text")
    }

private val curatedPromptHints = mapOf(
    "Pingwin" to "Antarktyda",
    "Foka" to "Wąsy",
    "Renifer" to "Sanie",
    "Lis polarny" to "Biel",
    "Sowa śnieżna" to "Noc",
    "Niedźwiedź polarny" to "Lód",
    "Drzewo" to "Liść",
    "Las" to "Ścieżka",
    "Plaża" to "Parasol",
    "Lotnisko" to "Walizka",
    "Biblioteka" to "Czytelnia",
    "Muzeum" to "Eksponat",
)

private fun buildPromptHints(categories: List<SeedCategory>): Map<String, String> = buildMap {
    categories.forEach { category ->
        val texts = category.prompts.mapIndexed { index, prompt ->
            prompt.toSeedPrompt(category.id, index).second
        }
        texts.forEachIndexed { index, text ->
            put(text, texts[(index + 1) % texts.size])
        }
    }
    putAll(curatedPromptHints)
}

private val seedJsonFormat = Json { ignoreUnknownKeys = true }

expect fun bundledCatalogRepository(): CatalogRepository
