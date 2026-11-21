package com.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SeedTest {

    @Test
    fun `seed has six areas`() {
        assertEquals(6, Seed.statements().count { it.startsWith("INSERT INTO muscle_area") })
    }

    @Test
    fun `seed covers catalog, muscles, links, preset groups`() {
        val statements = Seed.statements()
        assertEquals(15, statements.count { it.startsWith("INSERT INTO muscle (") })
        assertEquals(46, statements.count { it.startsWith("INSERT INTO exercise ") })
        // У каждого упражнения ровно одна PRIMARY-связь.
        assertEquals(46, statements.count { it.contains("'PRIMARY'") })
        assertEquals(4, statements.count { it.startsWith("INSERT INTO change_group") })
        // Каждая связка ссылается на существующую мышцу.
        val muscleIds = (1L..15L).toSet()
        statements.filter { it.contains("exercise_muscle") }.forEach { s ->
            val muscleId = Regex("VALUES \\(\\d+, (\\d+),").find(s)!!.groups[1]!!.value.toLong()
            assert(muscleId in muscleIds) { "несуществующая мышца: $muscleId" }
        }
    }

    @Test
    fun `preset groups are the four app-defined`() {
        val groups = Seed.statements().filter { it.startsWith("INSERT INTO change_group") }
        assert(groups.any { "Вид спереди" in it })
        assert(groups.any { "Вид сзади" in it })
        assert(groups.any { "Вид слева" in it })
        assert(groups.any { "Вид справа" in it })
    }
}
