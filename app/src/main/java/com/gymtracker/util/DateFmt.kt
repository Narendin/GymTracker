package com.gymtracker.util

import java.time.Instant
import java.time.ZoneId

private val RU_MONTHS = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

/** «19 ноября 2026». */
fun formatDateFull(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val d = Instant.ofEpochMilli(millis).atZone(zone)
    return "${d.dayOfMonth} ${RU_MONTHS[d.monthValue - 1]} ${d.year}"
}

/** «19 ноября» — значение счётчика последней тренировки. */
fun formatDateShort(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val d = Instant.ofEpochMilli(millis).atZone(zone)
    return "${d.dayOfMonth} ${RU_MONTHS[d.monthValue - 1]}"
}
