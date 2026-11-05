package com.impostor.app

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

actual fun currentLocalMoment(): LocalMoment {
    val units = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or NSCalendarUnitWeekday
    val c = NSCalendar.currentCalendar.components(units, fromDate = NSDate())
    return LocalMoment(
        year = c.year.toInt(),
        month = c.month.toInt(),
        day = c.day.toInt(),
        hour = c.hour.toInt(),
        isoDayOfWeek = (c.weekday.toInt() + 5) % 7 + 1,
    )
}
