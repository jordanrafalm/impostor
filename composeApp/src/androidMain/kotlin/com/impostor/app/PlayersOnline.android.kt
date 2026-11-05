package com.impostor.app

import java.util.Calendar

actual fun currentLocalMoment(): LocalMoment {
    val c = Calendar.getInstance()
    return LocalMoment(
        year = c.get(Calendar.YEAR),
        month = c.get(Calendar.MONTH) + 1,
        day = c.get(Calendar.DAY_OF_MONTH),
        hour = c.get(Calendar.HOUR_OF_DAY),
        isoDayOfWeek = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1,
    )
}
