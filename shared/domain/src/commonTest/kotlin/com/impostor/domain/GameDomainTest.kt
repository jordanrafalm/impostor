package com.impostor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GameDomainTest {
    @Test
    fun `should normalize player names`() {
        assertEquals("Ala", normalizePlayerName("  ala  "))
        assertEquals("Łukasz", normalizePlayerName(" łukasz"))
        assertEquals("Gracz 1", normalizePlayerName(" gracz 1 "))
        assertEquals("", normalizePlayerName("   "))
    }

    @Test
    fun seededStartIsDeterministicAndHasDistinctImpostors() {
        val setup = GameSetup((1..4).map { Player("p$it", "Player $it") }, setOf("animals"), 1)
        val prompts = listOf(Prompt("a-1", "animals", "Pingwin"))
        val result = StartGameUseCase(RandomSource { 0 }).invoke(setup, prompts, "fixed")
        assertEquals("fixed", result.sessionId)
        assertEquals(Role.IMPOSTOR, result.assignments.first().role)
        assertEquals(1, result.assignments.count { it.role == Role.IMPOSTOR })
    }

    @Test
    fun `should assign distinct impostors when random source returns the same index`() {
        // Given
        val setup = GameSetup((1..4).map { Player("p$it", "Player $it") }, setOf("animals"), 2)
        val prompts = listOf(Prompt("a-1", "animals", "Pingwin"))

        // When
        val result = StartGameUseCase(RandomSource { 0 }).invoke(setup, prompts)

        // Then
        assertEquals(2, result.assignments.count { it.role == Role.IMPOSTOR })
    }

    @Test
    fun `should carry hints setting from setup to session`() {
        // Given
        val setup = GameSetup(
            players = (1..3).map { Player("p$it", "Player $it") },
            selectedCategoryIds = setOf("animals"),
            impostorCount = 1,
            hintsEnabled = true,
        )

        // When
        val session = StartGameUseCase(RandomSource { 0 }).invoke(
            setup,
            listOf(Prompt("a-1", "animals", "Pingwin", "Zimowe zwierzę")),
        )

        // Then
        assertTrue(session.hintsEnabled)
    }

    @Test
    fun `should keep hints disabled by default`() {
        // Given
        val setup = GameSetup((1..3).map { Player("p$it", "Player $it") }, setOf("animals"), 1)

        // When
        val session = StartGameUseCase(RandomSource { 0 }).invoke(
            setup,
            listOf(Prompt("a-1", "animals", "Pingwin", "Zimowe zwierzę")),
        )

        // Then
        assertFalse(session.hintsEnabled)
    }

    @Test
    fun `should support different hints for different prompts`() {
        // Given
        val first = Prompt("a-1", "animals", "Pingwin", "Zimowe zwierzę")
        val second = Prompt("a-2", "animals", "Foka", "Zwierzę z wąsami")

        // Then
        assertNotEquals(first.hint, second.hint)
    }

    @Test
    fun `should show prompt hint to impostor when hints are enabled`() {
        // Given
        val prompt = Prompt("a-1", "animals", "Pingwin", "Zimowe zwierzę")

        // Then
        assertEquals("Zimowe zwierzę", prompt.revealText(Role.IMPOSTOR, hintsEnabled = true))
    }

    @Test
    fun `should hide prompt hint from impostor when hints are disabled`() {
        // Given
        val prompt = Prompt("a-1", "animals", "Pingwin", "Zimowe zwierzę")

        // Then
        assertEquals("IMPOSTOR", prompt.revealText(Role.IMPOSTOR, hintsEnabled = false))
    }

    @Test
    fun validationRequiresThreeUniqueNamedPlayersAndOneCategory() {
        val validator = ValidateGameSetupUseCase()
        val player = Player("1", "A")
        assertTrue(validator.invoke(GameSetup(listOf(player, player), setOf("x"), 1)) is SetupValidation.Invalid)
        assertEquals(
            SetupValidation.Invalid(SetupValidation.Reason.NO_CATEGORY),
            validator.invoke(GameSetup(listOf(Player("1", "A"), Player("2", "B"), Player("3", "C")), emptySet(), 1)),
        )
    }
}
