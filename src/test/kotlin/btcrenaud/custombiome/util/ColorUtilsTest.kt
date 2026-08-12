package btcrenaud.custombiome.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorUtilsTest {

    @Test
    fun `parses the three accepted hex prefixes identically`() {
        val expected = 0x1A3D2E
        assertEquals(expected, ColorUtils.parseHexColor("#1a3d2e"))
        assertEquals(expected, ColorUtils.parseHexColor("1A3D2E"))
        assertEquals(expected, ColorUtils.parseHexColor("0x1a3d2e"))
        assertEquals(expected, ColorUtils.parseHexColor("0X1A3D2E"))
    }

    @Test
    fun `parses white without overflowing into a negative int`() {
        assertEquals(0xFFFFFF, ColorUtils.parseHexColor("#ffffff"))
    }

    @Test
    fun `treats blank input as inherit rather than black`() {
        // The entry contract is "leave empty to inherit from the base biome", so an
        // empty field must stay null and never collapse to 0x000000.
        assertNull(ColorUtils.parseHexColor(""))
        assertNull(ColorUtils.parseHexColor("   "))
        assertNull(ColorUtils.parseHexColor(null))
    }

    @Test
    fun `rejects malformed colors`() {
        assertNull(ColorUtils.parseHexColor("#12345"))
        assertNull(ColorUtils.parseHexColor("#1234567"))
        assertNull(ColorUtils.parseHexColor("#gggggg"))
        assertNull(ColorUtils.parseHexColor("not a color"))
    }

    @Test
    fun `hex round trip is stable`() {
        val original = "#1A3D2E"
        val parsed = ColorUtils.parseHexColor(original)!!
        assertEquals(original, ColorUtils.toHexString(parsed))
    }

    @Test
    fun `hex string pads short values to six digits`() {
        assertEquals("#00FF00", ColorUtils.toHexString(0x00FF00))
        assertEquals("#000000", ColorUtils.toHexString(0))
    }

    @Test
    fun `validity mirrors parsing`() {
        assertTrue(ColorUtils.isValidHexColor("#abcdef"))
        assertFalse(ColorUtils.isValidHexColor("#abcde"))
    }
}
