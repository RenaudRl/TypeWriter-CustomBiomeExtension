package btcrenaud.custombiome.service

import btcrenaud.custombiome.util.BiomePacketHelper
import btcrenaud.custombiome.util.Scheduling
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Biome
import kotlin.math.max

/**
 * Writes biomes into the world.
 *
 * Two things drive this implementation. Biomes are stored per 4×4×4 quart, so writing every block
 * of a column does the same work 64 times over; and on a regionised server a write must happen on
 * the thread that owns the target chunk, so the work is split per chunk and scheduled there.
 */
object BiomePainter {

    /** Minecraft stores one biome per 4×4×4 cell. */
    const val QUART = 4

    data class PaintResult(
        val quartsWritten: Int,
        val chunksTouched: Int,
    ) {
        val isEmpty: Boolean get() = quartsWritten == 0
    }

    /**
     * Paints [biome] around [center] within [radius] blocks, over the world's full height.
     * The work is scheduled per chunk; the returned counts describe what was queued.
     */
    fun paintRadius(center: Location, biome: Biome, radius: Int): PaintResult {
        val world = center.world ?: return PaintResult(0, 0)
        return paintPositions(world, biome, radiusPositions(center, radius))
    }

    /**
     * The quart cells a radius paint would cover, over the world's full height.
     *
     * Exposed separately so callers can read the existing biomes before overwriting them — a
     * snapshot has to describe exactly the cells that are about to change.
     */
    fun radiusPositions(center: Location, radius: Int): List<Triple<Int, Int, Int>> {
        val world = center.world ?: return emptyList()
        val blockRadius = max(0, radius)
        val radiusSquared = blockRadius.toLong() * blockRadius.toLong()

        val positions = mutableListOf<Triple<Int, Int, Int>>()

        // Iterate on quart cells rather than on blocks: one entry per stored cell.
        val minX = alignDown(center.blockX - blockRadius)
        val maxX = center.blockX + blockRadius
        val minZ = alignDown(center.blockZ - blockRadius)
        val maxZ = center.blockZ + blockRadius

        var x = minX
        while (x <= maxX) {
            var z = minZ
            while (z <= maxZ) {
                val dx = (x - center.blockX).toLong()
                val dz = (z - center.blockZ).toLong()
                if (blockRadius == 0 || dx * dx + dz * dz <= radiusSquared) {
                    var y = alignDown(world.minHeight)
                    while (y < world.maxHeight) {
                        positions += Triple(x, y, z)
                        y += QUART
                    }
                }
                z += QUART
            }
            x += QUART
        }

        return positions
    }

    /**
     * Paints [biome] on an explicit set of quart positions, already grouped by the caller.
     * Used by region painting, where the shape comes from a selection rather than a radius.
     */
    fun paintPositions(world: World, biome: Biome, positions: Collection<Triple<Int, Int, Int>>): PaintResult {
        val byChunk = HashMap<Pair<Int, Int>, MutableList<Triple<Int, Int, Int>>>()
        positions.forEach { position ->
            byChunk.getOrPut((position.first shr 4) to (position.third shr 4)) { mutableListOf() } += position
        }
        return dispatch(world, biome, byChunk, positions.size)
    }

    private fun dispatch(
        world: World,
        biome: Biome,
        byChunk: Map<Pair<Int, Int>, List<Triple<Int, Int, Int>>>,
        quarts: Int,
    ): PaintResult {
        // Remember what we changed, so a later refresh re-asserts only these chunks.
        PaintedChunks.record(world, byChunk.keys)

        byChunk.forEach { (chunk, positions) ->
            val anchor = Location(
                world,
                (chunk.first shl 4).toDouble(),
                world.minHeight.toDouble(),
                (chunk.second shl 4).toDouble(),
            )
            Scheduling.runAtLocation(anchor) {
                positions.forEach { (x, y, z) -> world.setBiome(x, y, z, biome) }
                BiomePacketHelper.sendBiomeUpdate(world, setOf(chunk))
            }
        }
        return PaintResult(quarts, byChunk.size)
    }

    /** Floors to the quart grid, including for negative coordinates. */
    private fun alignDown(value: Int): Int = Math.floorDiv(value, QUART) * QUART
}
