package com.impostor.domain

data class Category(
    val id: String,
    val displayName: String,
    val iconKey: String,
    val isUnlocked: Boolean = true,
)

data class Prompt(
    val id: String,
    val categoryId: String,
    val text: String,
    val hint: String = "",
)

fun Prompt.revealText(role: Role, hintsEnabled: Boolean): String = when {
    role == Role.AGENT -> text
    hintsEnabled -> hint
    else -> "IMPOSTOR"
}

enum class Role { AGENT, IMPOSTOR }

fun normalizePlayerName(name: String): String = name.trim().replaceFirstChar { it.titlecase() }

data class Player(val id: String, val displayName: String)

data class Assignment(val playerId: String, val role: Role)

data class GameSetup(
    val players: List<Player>,
    val selectedCategoryIds: Set<String>,
    val impostorCount: Int,
    val hintsEnabled: Boolean = false,
)

data class GameSession(
    val sessionId: String,
    val prompt: Prompt,
    val assignments: List<Assignment>,
    val starterPlayerId: String,
    val revealIndex: Int = 0,
    val hintsEnabled: Boolean = false,
)

interface CatalogRepository {
    fun categories(): List<Category>
    fun prompts(categoryIds: Set<String>): List<Prompt>
}

fun interface RandomSource {
    fun nextInt(until: Int): Int
}

sealed interface SetupValidation {
    data object Valid : SetupValidation
    data class Invalid(val reason: Reason) : SetupValidation

    enum class Reason {
        TOO_FEW_PLAYERS,
        NO_CATEGORY,
        INVALID_IMPOSTOR_COUNT,
        DUPLICATE_PLAYER_NAMES,
        EMPTY_PLAYER_NAME,
    }
}

class LoadCatalogUseCase(private val repository: CatalogRepository) {
    fun invoke(): Pair<List<Category>, List<Prompt>> {
        val categories = repository.categories()
        return categories to repository.prompts(categories.map { it.id }.toSet())
    }
}

class ValidateGameSetupUseCase {
    fun invoke(setup: GameSetup): SetupValidation {
        if (setup.players.size < 3) return SetupValidation.Invalid(SetupValidation.Reason.TOO_FEW_PLAYERS)
        if (setup.selectedCategoryIds.isEmpty()) return SetupValidation.Invalid(SetupValidation.Reason.NO_CATEGORY)
        if (setup.impostorCount !in 1 until setup.players.size) {
            return SetupValidation.Invalid(SetupValidation.Reason.INVALID_IMPOSTOR_COUNT)
        }
        if (setup.players.any { it.displayName.trim().isEmpty() }) {
            return SetupValidation.Invalid(SetupValidation.Reason.EMPTY_PLAYER_NAME)
        }
        if (setup.players.map { it.displayName.trim().lowercase() }.toSet().size != setup.players.size) {
            return SetupValidation.Invalid(SetupValidation.Reason.DUPLICATE_PLAYER_NAMES)
        }
        return SetupValidation.Valid
    }
}

class StartGameUseCase(private val random: RandomSource) {
    fun invoke(setup: GameSetup, prompts: List<Prompt>, sessionId: String = "local-session"): GameSession {
        check(ValidateGameSetupUseCase().invoke(setup) == SetupValidation.Valid)
        val selectedPrompts = prompts.filter { it.categoryId in setup.selectedCategoryIds }
        check(selectedPrompts.isNotEmpty()) { "No prompts available for selected categories" }
        val prompt = selectedPrompts[random.nextIndex(selectedPrompts.size)]
        val availablePlayerIndices = setup.players.indices.toMutableList()
        val impostors = buildSet {
            repeat(setup.impostorCount) {
                add(availablePlayerIndices.removeAt(random.nextIndex(availablePlayerIndices.size)))
            }
        }
        val assignments = setup.players.mapIndexed { index, player ->
            Assignment(player.id, if (index in impostors) Role.IMPOSTOR else Role.AGENT)
        }
        return GameSession(
            sessionId = sessionId,
            prompt = prompt,
            assignments = assignments,
            starterPlayerId = setup.players[random.nextIndex(setup.players.size)].id,
            hintsEnabled = setup.hintsEnabled,
        )
    }

    private fun RandomSource.nextIndex(until: Int): Int {
        val index = nextInt(until)
        require(index in 0 until until) { "RandomSource returned $index for range 0 until $until" }
        return index
    }
}

class RevealNextPlayerUseCase {
    fun invoke(session: GameSession, revealed: Boolean): GameSession {
        if (!revealed || session.revealIndex >= session.assignments.lastIndex) return session
        return session.copy(revealIndex = session.revealIndex + 1)
    }
}

class FinishRevealUseCase {
    fun invoke(session: GameSession): Boolean = session.revealIndex == session.assignments.lastIndex
}
