package btcrenaud.custombiome.injector

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Stand-ins for the JOML interfaces: the production code recognises them by simple name only. */
private interface Vector3fc
private interface Vector4fc

private class Attribute<Value>

/** Mimics the constants of `EnvironmentAttributes` across server versions. */
@Suppress("unused")
private object AttributeConstants {
    @JvmField val legacyColor: Attribute<Int> = Attribute()
    @JvmField val rgbColor: Attribute<Vector3fc> = Attribute()
    @JvmField val argbColor: Attribute<Vector4fc> = Attribute()
    @JvmField val distance: Attribute<Float> = Attribute()
    @JvmField val particleList: Attribute<List<String>> = Attribute()
    @JvmField val rawType: Attribute<*>? = null
}

/** A server class before the spawn getter was removed, and after. */
private class BiomeWithGetter {
    @Suppress("unused")
    fun getMobSettings(): String = "spawns"
}

private class BiomeWithoutGetter

class AttributeValuesTest {

    private fun constant(name: String) = AttributeConstants::class.java.getField(name)

    @Test
    fun `a legacy integer colour attribute stays packed`() {
        val valueClass = AttributeValues.valueClassOf(constant("legacyColor"))

        assertEquals(ColorForm.PACKED_INT, AttributeValues.colorFormOf(valueClass))
    }

    @Test
    fun `a Vector3fc attribute is read as an RGB vector`() {
        val valueClass = AttributeValues.valueClassOf(constant("rgbColor"))

        assertEquals(ColorForm.RGB_VECTOR, AttributeValues.colorFormOf(valueClass))
    }

    @Test
    fun `a Vector4fc attribute is read as an ARGB vector`() {
        val valueClass = AttributeValues.valueClassOf(constant("argbColor"))

        assertEquals(ColorForm.ARGB_VECTOR, AttributeValues.colorFormOf(valueClass))
    }

    @Test
    fun `an unreadable declared type falls back to a packed integer`() {
        assertNull(AttributeValues.valueClassOf(constant("rawType")))
        assertNull(AttributeValues.valueClassOf(constant("particleList")))
        assertEquals(ColorForm.PACKED_INT, AttributeValues.colorFormOf(null))
    }

    @Test
    fun `a non colour attribute type is never mistaken for a vector`() {
        val valueClass = AttributeValues.valueClassOf(constant("distance"))

        assertEquals(ColorForm.PACKED_INT, AttributeValues.colorFormOf(valueClass))
    }

    @Test
    fun `opaque keeps the colour and forces full alpha`() {
        assertEquals(0xFF78A7FF.toInt(), AttributeValues.opaque(0x78A7FF))
        assertEquals(0xFF000000.toInt(), AttributeValues.opaque(0))
    }

    @Test
    fun `opaque ignores any alpha already present`() {
        assertEquals(0xFF123456.toInt(), AttributeValues.opaque(0x12123456))
    }

    @Test
    fun `the spawn getter is found when the server has it`() {
        val getter = AttributeValues.methodOrNull(BiomeWithGetter::class.java, "getMobSettings")

        assertNotNull(getter)
        assertEquals("spawns", getter.invoke(BiomeWithGetter()))
    }

    @Test
    fun `the spawn getter is absent on a server that dropped it`() {
        assertNull(AttributeValues.methodOrNull(BiomeWithoutGetter::class.java, "getMobSettings"))
    }
}
