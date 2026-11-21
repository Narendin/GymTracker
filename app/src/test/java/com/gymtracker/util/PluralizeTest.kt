package com.gymtracker.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PluralizeTest {

    @Test
    fun `training plurals`() {
        assertEquals("тренировка", plural(1, "тренировка", "тренировки", "тренировок"))
        assertEquals("тренировки", plural(2, "тренировка", "тренировки", "тренировок"))
        assertEquals("тренировок", plural(5, "тренировка", "тренировки", "тренировок"))
        assertEquals("тренировок", plural(11, "тренировка", "тренировки", "тренировок"))
        assertEquals("тренировка", plural(21, "тренировка", "тренировки", "тренировок"))
        assertEquals("тренировки", plural(22, "тренировка", "тренировки", "тренировок"))
        assertEquals("тренировок", plural(25, "тренировка", "тренировки", "тренировок"))
    }

    @Test
    fun `labels`() {
        assertEquals("12 тренировок", workoutsLabel(12))
        assertEquals("1 тренировка", workoutsLabel(1))
        assertEquals("3 упражнения", exercisesLabel(3))
        assertEquals("6 упражнений", exercisesLabel(6))
        assertEquals("1 упражнение", exercisesLabel(1))
    }

    @Test
    fun `grouped numbers`() {
        assertEquals("1 495", groupNumber(1495))
        assertEquals("23", groupNumber(23))
        assertEquals("10 000", groupNumber(10000))
        assertEquals("1 060", groupNumber(1060))
    }

    @Test
    fun `weight formatting`() {
        assertEquals("40", formatWeight(40.0))
        assertEquals("22,5", formatWeight(22.5))
        assertEquals("2,8", formatWeight(2.75))
        assertEquals("0", formatWeight(0.0))
    }

    @Test
    fun `russian dates`() {
        val utc = ZoneId.of("UTC")
        val millis = Instant.parse("2026-11-19T12:00:00Z").toEpochMilli()
        assertEquals("19 ноября 2026", formatDateFull(millis, utc))
        assertEquals("19 ноября", formatDateShort(millis, utc))
        val jan = Instant.parse("2026-01-05T09:00:00Z").toEpochMilli()
        assertEquals("5 января 2026", formatDateFull(jan, utc))
    }
}
