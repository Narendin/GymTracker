package com.gymtracker.update

import com.gymtracker.util.formatDateFull
import java.time.Instant
import java.time.ZoneId

/**
 * Сравнение номеров версий: текущей и последней на стороне релизов.
 *
 * Возвращает 1, если [latest] новее [current], -1 если старше, 0 если равны.
 * Возвращает null, когда номер не читается: приложение не имеет права
 * утверждать что-либо о новой версии и не показывает результат.
 *
 * Нули справа не значат — «1.0» и «1.0.0» равны. Префикс «v» не значим.
 */
fun compareVersions(current: String, latest: String): Int? {
    val a = parseVersion(current) ?: return null
    val b = parseVersion(latest) ?: return null
    val length = maxOf(a.size, b.size)
    for (i in 0 until length) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return if (y > x) 1 else -1
    }
    return 0
}

/**
 * Номер как список чисел: «1.0.2» → [1, 0, 2].
 * Любая часть, не являющаяся целым («1.0-rc1», «1..2», пустая строка),
 * делает номер нечитаемым.
 */
private fun parseVersion(raw: String): List<Int>? {
    val body = raw.trim().removePrefix("v").removePrefix("V")
    if (body.isEmpty()) return null
    return body.split(".").map { part -> part.toIntOrNull() ?: return null }
}

/**
 * Дата релиза из ответа релизной стороны («2026-11-23T22:12:41Z») — в
 * человеческий вид «23 ноября 2026». Нечитаемый ответ даёт пустую строку,
 * чтобы диалог обходился без даты.
 */
fun formatReleaseDate(iso: String, zone: ZoneId = ZoneId.systemDefault()): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
    return formatDateFull(instant.toEpochMilli(), zone)
}
