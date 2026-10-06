package btcrenaud.custombiome.service

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiomeExplorationTest {

    private val required = listOf("typewriter:corrupted_forest", "typewriter:ash_plains")

    private fun complete(discovered: Set<String>, requireAll: Boolean, asked: List<String> = required) =
        BiomeExploration.isComplete(asked, { it in discovered }, requireAll)

    @Test
    fun `any one listed biome is enough when not every biome is required`() {
        assertTrue(complete(setOf("typewriter:ash_plains"), requireAll = false))
    }

    @Test
    fun `a biome that is not listed does not count`() {
        assertFalse(complete(setOf("minecraft:plains"), requireAll = false))
    }

    @Test
    fun `every listed biome must be discovered when all are required`() {
        assertFalse(complete(setOf("typewriter:ash_plains"), requireAll = true))
        assertTrue(complete(setOf("typewriter:ash_plains", "typewriter:corrupted_forest"), requireAll = true))
    }

    @Test
    fun `extra discoveries do not matter`() {
        val discovered = setOf("typewriter:ash_plains", "typewriter:corrupted_forest", "minecraft:desert")

        assertTrue(complete(discovered, requireAll = true))
    }

    @Test
    fun `an objective that asks for no biome is never complete`() {
        assertFalse(complete(setOf("typewriter:ash_plains"), requireAll = false, asked = emptyList()))
        assertFalse(complete(setOf("typewriter:ash_plains"), requireAll = true, asked = emptyList()))
    }
}
