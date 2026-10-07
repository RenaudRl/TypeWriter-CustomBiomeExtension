package btcrenaud.custombiome.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackMetadataTest {

    private val description = "Custom biomes"

    private fun pack(format: Int, limit: Int = 81) =
        PackMetadata.json(description, format, limit).getAsJsonObject("pack")

    @Test
    fun `a format above the legacy limit declares min and max format`() {
        val pack = pack(format = 121)

        assertEquals(121, pack.get("min_format").asInt)
        assertEquals(121, pack.get("max_format").asInt)
    }

    @Test
    fun `a format above the legacy limit no longer writes a bare pack_format`() {
        assertFalse(pack(format = 121).has("pack_format"))
    }

    @Test
    fun `the first format after the limit already needs min and max format`() {
        val pack = pack(format = 82)

        assertTrue(pack.has("min_format") && pack.has("max_format"))
    }

    @Test
    fun `a format at the legacy limit keeps the bare pack_format`() {
        val pack = pack(format = 81)

        assertEquals(81, pack.get("pack_format").asInt)
        assertFalse(pack.has("min_format"))
        assertFalse(pack.has("max_format"))
    }

    @Test
    fun `the limit read from the server decides, not a constant`() {
        assertTrue(pack(format = 90, limit = 100).has("pack_format"))
        assertTrue(pack(format = 90, limit = 81).has("min_format"))
    }

    @Test
    fun `the description is carried over`() {
        assertEquals(description, pack(format = 121).get("description").asString)
    }
}
