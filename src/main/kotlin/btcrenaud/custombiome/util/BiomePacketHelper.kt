package btcrenaud.custombiome.util

import btcrenaud.custombiome.registry.CustomBiomeRegistry
import btcrenaud.custombiome.service.PaintedChunks
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette
import com.github.retrooper.packetevents.util.Vector2i
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkBiomes
import org.bukkit.World
import org.bukkit.entity.Player
import org.slf4j.LoggerFactory
import kotlin.math.abs

/**
 * Pushes biome changes to clients.
 *
 * This used to send chunk *unload* packets. The protocol is server-push: the client dropped the
 * chunk and nothing ever sent it back, leaving a hole in the world until the player walked out of
 * view distance and back. The correct packet is `ChunkBiomes`, which replaces biome data in place
 * without touching blocks or entities.
 */
object BiomePacketHelper {

    private val logger = LoggerFactory.getLogger(BiomePacketHelper::class.java)

    /** Sends the current biome data of [chunks] to every player who can see them. */
    fun sendBiomeUpdate(world: World, chunks: Set<Pair<Int, Int>>) {
        if (chunks.isEmpty()) return

        for (player in world.players) {
            val visible = chunks.filter { it.visibleTo(player) }.toSet()
            if (visible.isNotEmpty()) send(player, world, visible)
        }
    }

    /**
     * Re-sends biome data for the chunks this extension painted within [radius] chunks of [player].
     *
     * Untouched chunks are deliberately skipped. A biome packet carries one horizontal palette
     * repeated over every vertical section, so sending an untouched chunk would flatten its cave
     * biomes onto the surface one. Painted chunks have no such variation left to lose.
     *
     * Returns how many chunks were re-sent, so a caller can tell the difference between "nothing to
     * do" and "done".
     */
    fun refreshBiomesForPlayer(player: Player, radius: Int = 5): Int {
        val world = player.world
        val centerX = player.location.blockX shr 4
        val centerZ = player.location.blockZ shr 4

        val chunks = buildSet {
            for (cx in (centerX - radius)..(centerX + radius)) {
                for (cz in (centerZ - radius)..(centerZ + radius)) {
                    if (PaintedChunks.contains(world, cx, cz)) add(cx to cz)
                }
            }
        }

        if (chunks.isNotEmpty()) send(player, world, chunks)
        return chunks.size
    }

    /**
     * Shows [networkId] as the biome of [chunks] to a single player, without touching the world.
     * This is the whole basis of the per-player overlay.
     */
    fun sendSingleBiome(player: Player, world: World, chunks: Set<Pair<Int, Int>>, networkId: Int) {
        if (chunks.isEmpty()) return

        val sectionCount = (world.maxHeight - world.minHeight) / 16
        val data = chunks.associate { (cx, cz) ->
            Vector2i(cx, cz) to
                WrapperPlayServerChunkBiomes.ChunkBiomeData.createWithSingleBiome(networkId, sectionCount)
        }

        runCatching {
            PacketEvents.getAPI().playerManager.sendPacket(player, WrapperPlayServerChunkBiomes(data))
        }.onFailure { error ->
            logger.warn("Failed to send biome overlay to {}: {}", player.name, error.message)
        }
    }

    private fun send(player: Player, world: World, chunks: Set<Pair<Int, Int>>) {
        val sectionCount = (world.maxHeight - world.minHeight) / 16
        val data = HashMap<Vector2i, WrapperPlayServerChunkBiomes.ChunkBiomeData>()

        for ((cx, cz) in chunks) {
            val palette = chunkBiomePalette(world, cx, cz) ?: continue
            data[Vector2i(cx, cz)] =
                WrapperPlayServerChunkBiomes.ChunkBiomeData.createWithRepeatingPalette(palette, sectionCount)
        }

        if (data.isEmpty()) return

        runCatching {
            PacketEvents.getAPI().playerManager.sendPacket(player, WrapperPlayServerChunkBiomes(data))
        }.onFailure { error ->
            logger.warn("Failed to send biome update to {}: {}", player.name, error.message)
        }
    }

    /**
     * Builds the chunk's biome palette from what the world actually contains.
     *
     * A section holds 4×4×4 biome cells. Everything this extension paints covers the full column,
     * so the same horizontal layout is valid for every section and one repeating palette describes
     * the whole chunk exactly.
     *
     * An earlier version only handled chunks where every cell shared one biome, which is almost
     * never true after a partial paint — so the packet was silently skipped and nothing changed on
     * screen even though the world had been written.
     */
    private fun chunkBiomePalette(world: World, chunkX: Int, chunkZ: Int): DataPalette? {
        if (!world.isChunkLoaded(chunkX, chunkZ)) return null

        val palette = DataPalette.createForBiome()
        val baseX = chunkX shl 4
        val baseZ = chunkZ shl 4
        val sampleY = world.minHeight

        for (qx in 0 until CELLS) {
            for (qz in 0 until CELLS) {
                val biome = world.getBiome(baseX + qx * 4, sampleY, baseZ + qz * 4)
                val id = CustomBiomeRegistry.injector.networkId(biome.key) ?: return null
                for (qy in 0 until CELLS) palette.set(qx, qy, qz, id)
            }
        }

        return palette
    }

    /** A section stores 4×4×4 biome cells. */
    private const val CELLS = 4

    private fun Pair<Int, Int>.visibleTo(player: Player): Boolean {
        val px = player.location.blockX shr 4
        val pz = player.location.blockZ shr 4
        val distance = player.viewDistance
        return abs(first - px) <= distance && abs(second - pz) <= distance
    }
}
