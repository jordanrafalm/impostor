package com.impostor.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class BundledCatalogRepositoryTest {
  @Test
  fun `should expose exactly the ordered adult prompt pairs`() {
    // Given
    val repository = bundledCatalogRepository()
    val expectedPairs = listOf(
      "Pocałunek" to "Pierwszy",
      "Flirt" to "Wzrok",
      "Namiętność" to "Noc",
      "Pożądanie" to "Spojrzenie",
      "Czułość" to "Dłoń",
      "Szept" to "Ucho",
      "Bliskość" to "Oddech",
      "Hotel" to "Recepcja",
      "Masaż" to "Olejki",
      "Kąpiel" to "Piana",
      "Świece" to "Cień",
      "Bielizna" to "Szuflada",
      "Pończochy" to "Pasek",
      "Szpilki" to "Krok",
      "Zapach" to "Szyja",
      "Skóra" to "Ciepło",
      "Szyja" to "Całus",
      "Plecy" to "Dreszczyk",
      "Biodra" to "Rytm",
      "Uda" to "Miękkość",
      "Uwodzenie" to "Gra",
      "Kuszenie" to "Uśmiech",
      "Tajemnica" to "List",
      "Zakazany" to "Owoc",
      "Romans" to "Mąż",
      "Zdrada" to "Telefon",
      "Dreszczyk" to "Kręgosłup",
      "Ekscytacja" to "Adrenalina",
      "Podniecenie" to "Tętno",
      "Rozkosz" to "Westchnienie",
      "Całowanie" to "Wolno",
      "Przytulanie" to "Ramiona",
      "Głaskanie" to "Palce",
      "Gryzienie" to "Zęby",
      "Drapanie" to "Paznokcie",
      "Przyciąganie" to "Chemia",
      "Magnetyzm" to "Spojrzenie",
      "Iskra" to "Pierwszy",
      "Żar" to "Skóra",
      "Wilgoć" to "Oddech",
      "Gorąco" to "Policzki",
      "Cień" to "Ściana",
      "Balkon" to "Noc",
      "Deszcz" to "Szyba",
      "Aksamit" to "Dotyk",
      "Koronka" to "Brzeg",
      "Jedwab" to "Pościel",
      "Perfumy" to "Nadgarstek",
      "Szminka" to "Ślad",
      "Kolczyk" to "Ucho",
    )

    // When
    val category = repository.categories().single { it.id == "adult" }
    val prompts = repository.prompts(setOf("adult")).map { it.text to it.hint }

    // Then
    assertEquals("18+", category.displayName)
    assertEquals(50, prompts.size)
    assertEquals(expectedPairs, prompts)
  }

  @Test
  fun `should expose all ordered sex category pairs in the active catalog`() {
    // Given
    val repository = bundledCatalogRepository()
    val expectedPairs = listOf(
      "Laska" to "Kolana",
      "Gardło" to "Łzy",
      "Anal" to "Tyłek",
      "Podwójna" to "Trzy",
      "Creampie" to "Wewnątrz",
      "Facial" to "Twarz",
      "Squirt" to "Fontanna",
      "Wiązanie" to "Liny",
      "BDSM" to "Kajdanki",
      "Klapsy" to "Pośladki",
      "Pegging" to "Strap",
      "Roleplay" to "Kostium",
      "Tatuś" to "Daddy",
      "Zwierzątko" to "Obroża",
      "Rogacz" to "Cuckold",
      "Hotwife" to "Żona",
      "Trójkąt" to "Trzech",
      "Gangbang" to "Kolejka",
      "Publiczny" to "Ryzyko",
      "Quickie" to "Toaleta",
      "Auto" to "Parking",
      "Biuro" to "Biurko",
      "Prysznic" to "Mydło",
      "Lustro" to "Odbicie",
      "Opaska" to "Ciemność",
      "Kajdanki" to "Metal",
      "Bicz" to "Smagnięcie",
      "Klamerki" to "Sutki",
      "Wibrator" to "Wibracje",
      "Dildo" to "Silikon",
      "Korek" to "Tyłek",
      "Koraliki" to "Szereg",
      "Pierścień" to "Nacisk",
      "Edging" to "Krawędź",
      "Kontrola" to "Zakaz",
      "Wymuszony" to "Litość",
      "Zniszczony" to "Ruined",
      "Prostata" to "Gruczoł",
      "PunktG" to "Środek",
      "Trening" to "Odruch",
      "Mokra" to "Ślina",
      "Jaja" to "Ssanie",
      "Rimming" to "Tyłek",
      "Facesitting" to "Tron",
      "Sześćdziesiątdziewięć" to "Naprzeciw",
      "Piesek" to "Tyłem",
      "Misjonarz" to "Twarz",
      "Jeździec" to "Góra",
      "Odwrócony" to "Tył",
      "Prone" to "Brzuch",
    )

    // When
    val category = repository.categories().single { it.id == "sex" }
    val prompts = repository.prompts(setOf("sex")).map { it.text to it.hint }

    // Then
    assertEquals("SEX", category.displayName)
    assertEquals(50, expectedPairs.size)
    assertEquals(expectedPairs, prompts)
  }

  @Test
  fun `should provide complete premium celebrity categories`() {
    // Given
    val categories = mapOf(
            "polish_influencers" to ("Polscy influencerzy" to listOf(
        "Kuba Wojewódzki", "Karol Wiśniewski", "Weronika Sowa", "Michał Baron", "Marta Błoch",
        "Agata Fąk", "Julia Żugaj", "Maja Kuczyńska", "Lexy Chaplin", "Natsu",
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