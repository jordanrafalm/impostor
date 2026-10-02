package com.impostor.app

import androidx.compose.runtime.saveable.listSaver
import com.impostor.domain.Assignment
import com.impostor.domain.GameSession
import com.impostor.domain.Player
import com.impostor.domain.Prompt
import com.impostor.domain.Role
import com.impostor.domain.SetupValidation

internal val playerListSaver = listSaver<List<Player>, String>(
    save = { players -> players.flatMap { listOf(it.id, it.displayName) } },
    restore = { saved ->
        if (saved.size % 2 != 0) {
            null
        } else {
            saved.chunked(2).map { (id, displayName) -> Player(id, displayName) }
        }
    },
)

internal val stringSetSaver = listSaver<Set<String>, String>(
    save = { values -> values.toList() },
    restore = { values -> values.toSet() },
)

internal val setupValidationReasonSaver = listSaver<SetupValidation.Reason?, String>(
    save = { reason -> listOf(reason?.name.orEmpty()) },
    restore = { saved ->
        saved.singleOrNull()?.let { name ->
            SetupValidation.Reason.values().firstOrNull { it.name == name }
        }
    },
)

internal fun saveGameSession(session: GameSession): List<String> = buildList {
    add(session.sessionId)
    add(session.prompt.id)
    add(session.prompt.categoryId)
    add(session.prompt.text)
    add(session.prompt.hint)
    add(session.starterPlayerId)
    add(session.revealIndex.toString())
    add(session.hintsEnabled.toString())
    add(session.assignments.size.toString())
    session.assignments.forEach { assignment ->
        add(assignment.playerId)
        add(assignment.role.name)
    }
}

internal fun restoreGameSession(saved: List<String>): GameSession? {
    if (saved.size < 9) return null

    val revealIndex = saved[6].toIntOrNull()?.takeIf { it >= 0 } ?: return null
    val hintsEnabled = saved[7].toBooleanStrictOrNull() ?: return null
    val assignmentCount = saved[8].toIntOrNull()?.takeIf { it > 0 } ?: return null
    if (saved.size != 9 + assignmentCount * 2) return null

    val assignments = mutableListOf<Assignment>()
    for (index in 0 until assignmentCount) {
        val offset = 9 + index * 2
        val role = Role.values().firstOrNull { it.name == saved[offset + 1] } ?: return null
        assignments += Assignment(saved[offset], role)
    }
    if (revealIndex >= assignments.size) return null

    return GameSession(
        sessionId = saved[0],
        prompt = Prompt(
            id = saved[1],
            categoryId = saved[2],
            text = saved[3],
            hint = saved[4],
        ),
        assignments = assignments,
        starterPlayerId = saved[5],
        revealIndex = revealIndex,
        hintsEnabled = hintsEnabled,
    )
}
