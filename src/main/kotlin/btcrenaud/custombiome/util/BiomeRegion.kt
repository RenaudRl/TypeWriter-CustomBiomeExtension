package btcrenaud.custombiome.util

import com.typewritermc.core.utils.point.Position
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * A cuboid between two captured positions.
 *
 * The two corners are given in whatever order the builder captured them, so everything is
 * normalised once here rather than defended against at every call site.
 */
data class BiomeRegion(
    val world: String,
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
) {

    /** True when the point is inside. [ignoreY] turns the region into an infinite column. */
    fun contains(world: String, x: Int, y: Int, z: Int, ignoreY: Boolean = false): Boolean {
        if (world != this.world) return false
        if (x < minX || x > maxX) return false
        if (z < minZ || z > maxZ) return false
        return ignoreY || (y in minY..maxY)
    }

    /**
     * Every chunk the region touches.
     *
     * Overlays are delivered per chunk, so a region that cuts a chunk in half still repaints that
     * whole chunk. That is a property of the protocol, not a rounding choice made here.
     */
    fun chunks(): Set<Pair<Int, Int>> = buildSet {
        for (cx in (minX shr 4)..(maxX shr 4)) {
            for (cz in (minZ shr 4)..(maxZ shr 4)) {
                add(cx to cz)
            }
        }
    }

    /** How many chunks [chunks] would produce, without building the set. */
    val chunkCount: Int
        get() = ((maxX shr 4) - (minX shr 4) + 1) * ((maxZ shr 4) - (minZ shr 4) + 1)

    /**
     * True when this region belongs to the world identified by [uid] or [name].
     *
     * A stored world is normally a UUID: `WorldSerializer` resolves whatever a page holds into
     * one. A name is still accepted so a hand-written page keeps working.
     */
    fun matchesWorld(uid: String, name: String): Boolean =
        world == uid || world.equals(name, ignoreCase = true)

    companion object {
        /**
         * Builds a region from two captured positions, or null when they are unusable: either
         * corner never captured, or two different worlds.
         */
        fun of(cornerA: Position, cornerB: Position): BiomeRegion? {
            val world = cornerA.world.identifier
            if (world.isEmpty() || world != cornerB.world.identifier) return null
            if (cornerA.isUnset() || cornerB.isUnset()) return null

            val ax = block(cornerA.x)
            val ay = block(cornerA.y)
            val az = block(cornerA.z)
            val bx = block(cornerB.x)
            val by = block(cornerB.y)
            val bz = block(cornerB.z)

            return BiomeRegion(
                world = world,
                minX = min(ax, bx),
                minY = min(ay, by),
                minZ = min(az, bz),
                maxX = max(ax, bx),
                maxY = max(ay, by),
                maxZ = max(az, bz),
            )
        }

        private fun block(value: Double): Int = floor(value).toInt()

        /**
         * A corner nobody captured.
         *
         * Comparing against [Position.ORIGIN] is not enough: an empty world identifier is resolved
         * to the first loaded world when the page is read, so an untouched corner comes back as a
         * real world at 0,0,0 rather than as the origin. The coordinates are what stays reliable.
         */
        private fun Position.isUnset(): Boolean = x == 0.0 && y == 0.0 && z == 0.0
    }
}
