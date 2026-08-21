package btcrenaud.custombiome.util

import com.typewritermc.core.utils.point.Position
import com.typewritermc.core.utils.point.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BiomeRegionTest {

    private fun at(world: String, x: Double, y: Double, z: Double) = Position(World(world), x, y, z)

    @Test
    fun `corners are normalised whichever order they were captured in`() {
        val forward = BiomeRegion.of(at("w", 10.0, 70.0, 30.0), at("w", -5.0, 60.0, 5.0))
        val backward = BiomeRegion.of(at("w", -5.0, 60.0, 5.0), at("w", 10.0, 70.0, 30.0))

        assertNotNull(forward)
        assertEquals(forward, backward)
        assertEquals(-5, forward.minX)
        assertEquals(10, forward.maxX)
    }

    @Test
    fun `an uncaptured corner leaves no region`() {
        assertNull(BiomeRegion.of(Position.ORIGIN, at("w", 1.0, 1.0, 1.0)))
        assertNull(BiomeRegion.of(at("w", 1.0, 1.0, 1.0), Position.ORIGIN))
    }

    @Test
    fun `a corner at zero counts as uncaptured even with a resolved world`() {
        // Reading a page turns an empty world identifier into the first loaded world, so an
        // untouched corner never comes back equal to Position.ORIGIN. Only the coordinates tell.
        assertNull(BiomeRegion.of(at("w", 0.0, 0.0, 0.0), at("w", 20.0, 70.0, 20.0)))
    }

    @Test
    fun `a world matches by uuid or by name`() {
        val region = BiomeRegion.of(at("a1b2", 1.0, 1.0, 1.0), at("a1b2", 5.0, 5.0, 5.0))
        assertNotNull(region)
        assertTrue(region.matchesWorld("a1b2", "world"), "the stored uuid matches")
        assertTrue(region.matchesWorld("other-uuid", "A1B2"), "a hand-written name still matches")
        assertFalse(region.matchesWorld("other-uuid", "world"))
    }

    @Test
    fun `two worlds make no region`() {
        assertNull(BiomeRegion.of(at("overworld", 0.0, 0.0, 0.0), at("nether", 10.0, 10.0, 10.0)))
    }

    @Test
    fun `negative coordinates round down, not toward zero`() {
        // -0.5 sits in block -1. Truncating instead of flooring would place it in block 0 and shift
        // the whole region by one block on the negative side.
        val region = BiomeRegion.of(at("w", -0.5, 0.0, -0.5), at("w", 5.0, 5.0, 5.0))
        assertNotNull(region)
        assertEquals(-1, region.minX)
        assertEquals(-1, region.minZ)
    }

    @Test
    fun `containment respects every axis`() {
        val region = BiomeRegion.of(at("w", 0.0, 60.0, 0.0), at("w", 15.0, 70.0, 15.0))
        assertNotNull(region)

        assertTrue(region.contains("w", 5, 65, 5))
        assertTrue(region.contains("w", 0, 60, 0), "the low corner is inside")
        assertTrue(region.contains("w", 15, 70, 15), "the high corner is inside")
        assertFalse(region.contains("w", 16, 65, 5))
        assertFalse(region.contains("w", 5, 80, 5))
        assertFalse(region.contains("other", 5, 65, 5), "another world is never inside")
    }

    @Test
    fun `ignoring height makes an infinite column`() {
        val region = BiomeRegion.of(at("w", 0.0, 60.0, 0.0), at("w", 15.0, 70.0, 15.0))
        assertNotNull(region)

        assertFalse(region.contains("w", 5, 200, 5, ignoreY = false))
        assertTrue(region.contains("w", 5, 200, 5, ignoreY = true))
        assertTrue(region.contains("w", 5, -50, 5, ignoreY = true))
        assertFalse(region.contains("w", 40, 200, 5, ignoreY = true), "height is ignored, not the rest")
    }

    @Test
    fun `a region inside one chunk touches exactly that chunk`() {
        val region = BiomeRegion.of(at("w", 1.0, 60.0, 1.0), at("w", 14.0, 70.0, 14.0))
        assertNotNull(region)
        assertEquals(setOf(0 to 0), region.chunks())
        assertEquals(1, region.chunkCount)
    }

    @Test
    fun `a region straddling a chunk border covers both chunks whole`() {
        // The protocol paints per chunk: a region ending at x=16 repaints chunk 1 entirely.
        val region = BiomeRegion.of(at("w", 14.0, 60.0, 0.0), at("w", 16.0, 70.0, 0.0))
        assertNotNull(region)
        assertEquals(setOf(0 to 0, 1 to 0), region.chunks())
        assertEquals(region.chunkCount, region.chunks().size)
    }

    @Test
    fun `chunk count matches the enumerated chunks on negative coordinates`() {
        val region = BiomeRegion.of(at("w", -20.0, 60.0, -20.0), at("w", 20.0, 70.0, 20.0))
        assertNotNull(region)
        assertEquals(region.chunkCount, region.chunks().size)
        assertTrue((-2 to -2) in region.chunks())
    }
}
