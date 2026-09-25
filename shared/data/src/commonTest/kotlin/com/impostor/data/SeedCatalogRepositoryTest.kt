package com.impostor.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class SeedCatalogRepositoryTest {
  @Test
  fun `should provide complete premium celebrity categories`() {
    // Given
    val categories = mapOf(
            "polish_influencers" to ("Polscy influencerzy" to listOf(
        "Kuba Wojewódzki", "Karol Wiśniewski", "Weronika Sowa", "Michał Baron", "Marta Błoch",
        "Katarzyna Alexander", "Julia Żugaj", "Maja Kuczyńska", "Lexy Chaplin", "Natsu",
        "Kacper Błoński", "Monika Kociołek", "Kamil Labudda", "Michał Sikorski", "Dawid Kwiatkowski",
        "Andziaks", "Gimper", "Krzysztof Gonciarz", "Maffashion", "Deynn",
      )),
            "polish_actors" to ("Polscy aktorzy" to listOf(
        "Piotr Adamczyk", "Robert Więckiewicz", "Tomasz Kot", "Borys Szyc", "Maciej Stuhr",
        "Cezary Pazura", "Marek Kondrat", "Janusz Gajos", "Andrzej Seweryn", "Marcin Dorociński",
        "Jakub Gierszał", "Dawid Ogrodnik", "Agata Kulesza", "Kinga Preis", "Maja Ostaszewska",
        "Magdalena Cielecka", "Joanna Kulig", "Anna Dymna", "Danuta Stenka", "Beata Tyszkiewicz",
      )),
            "international_stars" to ("Zagraniczne gwiazdy" to listOf(
        "Taylor Swift", "Beyoncé", "Rihanna", "Lady Gaga", "Adele", "Justin Bieber",
        "Leonardo DiCaprio", "Brad Pitt", "Angelina Jolie", "Tom Cruise", "Dwayne Johnson",
        "Will Smith", "Jennifer Aniston", "Keanu Reeves", "Emma Watson", "Cristiano Ronaldo",
        "Lionel Messi", "Michael Jordan", "Oprah Winfrey", "Kim Kardashian",
      )),
    )
    val seedJson = """
      {
        "schemaVersion": 1,
        "activeCategoryIds": [${categories.keys.joinToString { "\"$it\"" }}],
        "categories": [
        ${categories.entries.joinToString(",") { (id, nameAndPrompts) ->
          val name = nameAndPrompts.first
          val prompts = nameAndPrompts.second
          "{\"id\":\"$id\",\"name\":\"$name\",\"iconKey\":\"sparkles\",\"prompts\":[" +
            prompts.mapIndexed { index, prompt ->
              "{\"id\":\"$id-${index + 1}\",\"text\":\"$prompt\",\"hint\":\"skojarzenie\"}"
            }.joinToString(",") + "]}"
        }}
        ]
      }
    """.trimIndent()
    val repository = SeedCatalogRepository(seedJson)

    // When
    val categoriesById = repository.categories().associateBy { it.id }
    val prompts = repository.prompts(categories.keys)

    // Then
    assertEquals(categories.mapValues { it.value.first }, categoriesById.mapValues { it.value.displayName })
    assertTrue(categories.all { (id, _) -> prompts.count { it.categoryId == id } >= 20 })
    assertEquals(prompts.size, prompts.map { it.text }.toSet().size)
    assertTrue(prompts.all { it.hint.isNotBlank() && it.hint != it.text })
  }

    @Test
    fun `should provide a non-empty semantic hint for every seeded prompt`() {
        // Given
        val repository = SeedCatalogRepository(
            """
            {
              "schemaVersion": 1,
              "activeCategoryIds": ["animals", "places"],
              "categories": [
                {"id": "animals", "name": "Zwierzęta", "iconKey": "paw", "prompts": ["Pingwin", "Foka"]},
                {"id": "places", "name": "Miejsca", "iconKey": "pin", "prompts": ["Lotnisko", "Drzewo"]}
              ]
            }
            """.trimIndent(),
        )

        // When
        val prompts = repository.prompts(setOf("animals", "places"))

        // Then
        prompts.forEach { prompt ->
            assertTrue(prompt.hint.isNotBlank(), "Missing hint for ${prompt.text}")
          assertNotEquals(
            if (prompt.categoryId == "animals") "Zwierzęta" else "Miejsca",
            prompt.hint,
          )
            assertFalse(prompt.hint.contains("znak", ignoreCase = true))
            assertFalse(prompt.hint.contains(prompt.text.length.toString()))
        }
        assertEquals("Antarktyda", prompts.first { it.text == "Pingwin" }.hint)
        assertEquals("Walizka", prompts.first { it.text == "Lotnisko" }.hint)
        assertEquals("Liść", prompts.first { it.text == "Drzewo" }.hint)
        assertTrue(prompts.all { it.hint != it.text })
    }
}