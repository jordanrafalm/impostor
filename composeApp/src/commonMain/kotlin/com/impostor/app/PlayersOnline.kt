package com.impostor.app

import kotlin.random.Random

data class LocalMoment(val year: Int, val month: Int, val day: Int, val hour: Int, val isoDayOfWeek: Int) {
    val dateLabel: String get() = "${day.toString().padStart(2, '0')}.${month.toString().padStart(2, '0')}.$year"
}

expect fun currentLocalMoment(): LocalMoment

fun playersOnlineRange(moment: LocalMoment): IntRange = when (moment.isoDayOfWeek) {
    5 -> if (moment.hour < 16) 2..7 else 10..17
    6, 7 -> if (moment.hour < 15) 4..7 else 7..17
    else -> if (moment.hour < 16) 2..5 else 4..7
}

fun playersOnline(moment: LocalMoment, random: Random = Random): Int = playersOnlineRange(moment).random(random)

private val categoryPopularity = listOf(
    "adult", "animals", "flirt", "polish_influencers", "places", "international_stars", "professions",
)

fun <T> sortByPopularity(items: List<T>, id: (T) -> String): List<T> =
    items.sortedBy { categoryPopularity.indexOf(id(it)).let { i -> if (i < 0) Int.MAX_VALUE else i } }
