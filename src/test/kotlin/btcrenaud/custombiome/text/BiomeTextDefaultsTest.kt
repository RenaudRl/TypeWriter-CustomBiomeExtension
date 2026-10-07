package btcrenaud.custombiome.text

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BiomeTextDefaultsTest {

    @Test
    fun `fills every placeholder of the region reply and keeps the wording admins already see`() {
        val reply = TextTemplate.fill(
            BiomeTextDefaults.REGION_DONE,
            mapOf("entry" to "Spawn zone", "min" to "1, 2, 3", "max" to "4, 5, 6", "world" to "world"),
        )

        assertEquals(
            "Region of <white>Spawn zone</white> set to <green>1, 2, 3</green> → <green>4, 5, 6</green> in world.",
            reply,
        )
    }

    @Test
    fun `replaces a placeholder at each place it appears in the biome list line`() {
        val line = TextTemplate.fill(
            BiomeTextDefaults.LIST_ENTRY,
            mapOf(
                "icon" to "*", "status" to "ok", "name" to "Corrupted Forest", "key" to "typewriter:corrupted_forest",
                "temperature" to "", "downfall" to "",
            ),
        )

        assertEquals(3, "typewriter:corrupted_forest".toRegex().findAll(line).count())
        assertFalse(line.contains('{'), "a placeholder was left unfilled: $line")
    }

    @Test
    fun `leaves a placeholder without a value as written`() {
        val reply = TextTemplate.fill(BiomeTextDefaults.APPLY_DONE, mapOf("biome" to "Plains"))

        assertEquals("Painting <blue>Plains</blue> over <green>{cells}</green> cell(s) across {chunks} chunk(s).", reply)
    }

    @Test
    fun `fills the placeholders of every default that takes one`() {
        val values = mapOf(
            "count" to "2", "strategy" to "s", "reason" to "r", "icon" to "i", "status" to "t", "name" to "n", "key" to "k",
            "temperature" to "0.5", "downfall" to "0.1", "value" to "v", "player" to "p", "entry" to "e",
            "failures" to "f", "min" to "a", "max" to "b", "world" to "w", "chunks" to "3", "biome" to "b",
            "cells" to "4",
        )
        val withPlaceholders = listOf(
            BiomeTextDefaults.LIST_HEADER, BiomeTextDefaults.LIST_STRATEGY, BiomeTextDefaults.STRATEGY_UNSUPPORTED,
            BiomeTextDefaults.LIST_ENTRY, BiomeTextDefaults.LIST_TEMPERATURE, BiomeTextDefaults.LIST_DOWNFALL,
            BiomeTextDefaults.INFO_HEADER, BiomeTextDefaults.INFO_NAME, BiomeTextDefaults.INFO_ID,
            BiomeTextDefaults.INFO_TEMPERATURE, BiomeTextDefaults.INFO_DOWNFALL, BiomeTextDefaults.INFO_BASE_BIOME,
            BiomeTextDefaults.REGION_PAGE_NOT_FOUND, BiomeTextDefaults.REGION_WRITE_FAILED, BiomeTextDefaults.REGION_DONE,
            BiomeTextDefaults.REFRESH_NONE, BiomeTextDefaults.REFRESH_DONE, BiomeTextDefaults.APPLY_UNKNOWN,
            BiomeTextDefaults.APPLY_DONE,
        )

        withPlaceholders.forEach { text ->
            assertFalse(TextTemplate.fill(text, values).contains('{'), "unfilled placeholder in: $text")
        }
    }

    @Test
    fun `shows a typed value literally instead of reading its tags`() {
        val typed = "<red>oops</red>"

        val shown = PlainTextComponentSerializer.plainText()
            .serialize(MiniMessage.miniMessage().deserialize(TextTemplate.safe(typed)))

        assertEquals(typed, shown)
    }
}
