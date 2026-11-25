package com.gymtracker.update

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class VersionCompareTest {

    @Test
    fun `newer version`() {
        assertEquals(1, compareVersions("1.0.2", "v1.0.3"))
        assertEquals(1, compareVersions("1.0.2", "1.1.0"))
        assertEquals(1, compareVersions("1.9.9", "2.0.0"))
    }

    @Test
    fun `same version`() {
        assertEquals(0, compareVersions("1.0.2", "v1.0.2"))
        assertEquals(0, compareVersions("1.0.2", "1.0.2"))
        // Нули справа не значат.
        assertEquals(0, compareVersions("1.0", "1.0.0"))
    }

    @Test
    fun `older version`() {
        assertEquals(-1, compareVersions("1.0.3", "v1.0.2"))
        assertEquals(-1, compareVersions("2.0.0", "1.9.9"))
    }

    @Test
    fun `unreadable numbers`() {
        assertEquals(null, compareVersions("1.0.2", "latest"))
        assertEquals(null, compareVersions("1.0.2", "1.0-rc1"))
        assertEquals(null, compareVersions("1.0.2", "1..2"))
        assertEquals(null, compareVersions("1.0.2", ""))
        assertEquals(null, compareVersions("", "1.0.2"))
    }

    @Test
    fun `release date`() {
        val utc = ZoneId.of("UTC")
        assertEquals("23 ноября 2026", formatReleaseDate("2026-11-23T22:12:41Z", utc))
        assertEquals("", formatReleaseDate("не дата", utc))
        assertEquals("", formatReleaseDate("", utc))
    }
}
