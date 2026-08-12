package btcrenaud.custombiome.service

import org.bukkit.World
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The chunks this extension has painted during the current session.
 *
 * Biome packets can only carry one horizontal palette repeated over every vertical section, so
 * sending a chunk flattens whatever vertical variation it had — cave biomes included. That is
 * harmless for chunks we painted, since a paint covers the full column and there is nothing left to
 * flatten, but it would visibly wreck untouched terrain.
 *
 * Tracking what we actually changed lets a refresh re-assert only that, and leave the rest alone.
 *
 * This is deliberately session-scoped: after a restart the world data is already correct on disk and
 * clients receive it through normal chunk loading, so there is nothing to re-assert.
 */
object PaintedChunks {

    private val painted = ConcurrentHashMap.newKeySet<Key>()

    private data class Key(val world: UUID, val x: Int, val z: Int)

    fun record(world: World, chunks: Collection<Pair<Int, Int>>) {
        chunks.forEach { (x, z) -> painted += Key(world.uid, x, z) }
    }

    fun contains(world: World, chunkX: Int, chunkZ: Int): Boolean =
        Key(world.uid, chunkX, chunkZ) in painted

    fun clear() = painted.clear()
}
