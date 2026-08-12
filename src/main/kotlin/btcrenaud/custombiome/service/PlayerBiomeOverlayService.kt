package btcrenaud.custombiome.service

import btcrenaud.custombiome.registry.CustomBiomeRegistry
import btcrenaud.custombiome.util.BiomePacketHelper
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import btcrenaud.custombiome.util.Scheduling
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Player
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-player biome overlays.
 *
 * A player can be shown a different biome from the one actually stored in the world: nothing is
 * written to disk, no other player is affected, and removing the overlay restores the real biome
 * instantly. This is what makes "the forest looks corrupted, but only during your quest" possible
 * without mutating a shared world.
 *
 * Chunks are re-asserted whenever the server sends the chunk again, so an overlay survives the
 * player walking out of view distance and back.
 */
object PlayerBiomeOverlayService {

    private val logger = LoggerFactory.getLogger(PlayerBiomeOverlayService::class.java)

    private data class ChunkPos(val world: UUID, val x: Int, val z: Int)

    /** player -> chunk -> biome shown instead of the real one. */
    private val overlays = ConcurrentHashMap<UUID, ConcurrentHashMap<ChunkPos, NamespacedKey>>()

    private var listener: ChunkListener? = null

    fun register() {
        if (listener != null) return
        runCatching {
            val created = ChunkListener()
            PacketEvents.getAPI().eventManager.registerListener(created)
            listener = created
            logger.info("Per-player biome overlay listener registered.")
        }.onFailure {
            logger.warn("PacketEvents unavailable: per-player biome overlays are disabled ({}).", it.message)
        }
    }

    fun unregister() {
        listener?.let { active ->
            runCatching { PacketEvents.getAPI().eventManager.unregisterListener(active) }
        }
        listener = null
        overlays.clear()
    }

    /** True when overlays can actually be delivered on this server. */
    val isAvailable: Boolean get() = listener != null

    /**
     * Shows [biome] to [player] on every chunk within [chunkRadius] of their position.
     * Returns the number of chunks now overlaid, or null when the biome has no network id.
     */
    fun apply(player: Player, biome: NamespacedKey, chunkRadius: Int): Int? {
        val networkId = CustomBiomeRegistry.injector.networkId(biome) ?: return null

        val world = player.world
        val centerX = player.location.blockX shr 4
        val centerZ = player.location.blockZ shr 4

        val chunks = buildSet {
            for (cx in (centerX - chunkRadius)..(centerX + chunkRadius)) {
                for (cz in (centerZ - chunkRadius)..(centerZ + chunkRadius)) {
                    add(cx to cz)
                }
            }
        }

        val forPlayer = overlays.getOrPut(player.uniqueId) { ConcurrentHashMap() }
        chunks.forEach { (cx, cz) -> forPlayer[ChunkPos(world.uid, cx, cz)] = biome }

        BiomePacketHelper.sendSingleBiome(player, world, chunks, networkId)
        return chunks.size
    }

    /** Drops every overlay of [player] and restores what the world really contains. */
    fun clear(player: Player) {
        val forPlayer = overlays.remove(player.uniqueId) ?: return

        val world = player.world
        val chunks = forPlayer.keys
            .filter { it.world == world.uid }
            .map { it.x to it.z }
            .toSet()

        BiomePacketHelper.sendBiomeUpdate(world, chunks)
    }

    fun forget(playerId: UUID) {
        overlays.remove(playerId)
    }

    /** The biome [player] is being shown at this position, or null when no overlay applies. */
    fun overlayAt(player: Player, world: World, blockX: Int, blockZ: Int): NamespacedKey? =
        overlays[player.uniqueId]?.get(ChunkPos(world.uid, blockX shr 4, blockZ shr 4))

    fun hasOverlay(player: Player): Boolean = overlays[player.uniqueId]?.isNotEmpty() == true

    /**
     * Re-applies the overlay right after the server sends a chunk the player has one for.
     * Without this the vanilla chunk data would win as soon as the chunk is resent.
     */
    private class ChunkListener : PacketListenerAbstract(PacketListenerPriority.LOW) {

        override fun onPacketSend(event: PacketSendEvent) {
            if (event.packetType != PacketType.Play.Server.CHUNK_DATA) return

            val player = event.getPlayer<Player>() ?: return
            val forPlayer = overlays[player.uniqueId] ?: return
            if (forPlayer.isEmpty()) return

            val column = WrapperPlayServerChunkData(event).column
            val world = player.world
            val biome = forPlayer[ChunkPos(world.uid, column.x, column.z)] ?: return
            val networkId = CustomBiomeRegistry.injector.networkId(biome) ?: return

            // The overlay has to arrive after the chunk itself, so it is queued rather than sent
            // from inside the send pipeline.
            val chunk = column.x to column.z
            Scheduling.runAsync {
                BiomePacketHelper.sendSingleBiome(player, world, setOf(chunk), networkId)
            }
        }
    }
}
