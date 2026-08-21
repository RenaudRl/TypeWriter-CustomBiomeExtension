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
import java.util.concurrent.atomic.AtomicLong

/**
 * Per-player biome overlays.
 *
 * A player can be shown a different biome from the one actually stored in the world: nothing is
 * written to disk, no other player is affected, and removing the overlay restores the real biome
 * instantly. This is what makes "the forest looks corrupted, but only during your quest" possible
 * without mutating a shared world.
 *
 * Overlays are held **per owner**. An owner is whatever asked for the overlay — an audience entry,
 * an action, a cinematic. Two owners can cover the same chunk; the one that applied last is shown,
 * and when it goes away the one underneath comes back rather than the whole stack collapsing.
 * Without this, a player leaving one overlay audience would silently strip every other overlay
 * they had.
 *
 * Chunks are re-asserted whenever the server sends the chunk again, so an overlay survives the
 * player walking out of view distance and back.
 */
object PlayerBiomeOverlayService {

    private val logger = LoggerFactory.getLogger(PlayerBiomeOverlayService::class.java)

    /** Owner used by the one-shot action, which has no entry-scoped lifetime of its own. */
    const val ACTION_OWNER = "action"

    data class ChunkPos(val world: UUID, val x: Int, val z: Int)

    /** What one owner shows to one player. Chunks may hold different biomes. */
    private class OwnerOverlay(val order: Long) {
        val chunks = ConcurrentHashMap<ChunkPos, NamespacedKey>()
    }

    /** player -> owner -> overlay. */
    private val overlays = ConcurrentHashMap<UUID, ConcurrentHashMap<String, OwnerOverlay>>()

    private val orderCounter = AtomicLong()

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

    /** Every chunk within [chunkRadius] of [center]. */
    fun chunksAround(world: World, centerX: Int, centerZ: Int, chunkRadius: Int): Set<ChunkPos> =
        buildSet {
            for (cx in (centerX - chunkRadius)..(centerX + chunkRadius)) {
                for (cz in (centerZ - chunkRadius)..(centerZ + chunkRadius)) {
                    add(ChunkPos(world.uid, cx, cz))
                }
            }
        }

    /**
     * Shows [biome] to [player] on every chunk within [chunkRadius] of their position, on behalf of
     * [owner]. Returns the number of chunks now overlaid, or null when the biome has no network id.
     */
    fun apply(
        player: Player,
        biome: NamespacedKey,
        chunkRadius: Int,
        owner: String = ACTION_OWNER,
    ): Int? {
        val world = player.world
        val chunks = chunksAround(
            world,
            player.location.blockX shr 4,
            player.location.blockZ shr 4,
            chunkRadius,
        )
        return apply(player, biome, chunks, owner)
    }

    /**
     * Shows [biome] to [player] over [chunks] on behalf of [owner].
     *
     * Applying replaces what that owner was showing; other owners are untouched. Returns the number
     * of chunks now overlaid by this owner, or null when the biome cannot be sent to a client.
     */
    fun apply(
        player: Player,
        biome: NamespacedKey,
        chunks: Set<ChunkPos>,
        owner: String,
    ): Int? {
        if (CustomBiomeRegistry.injector.networkId(biome) == null) return null
        if (chunks.isEmpty()) return 0

        val forPlayer = overlays.getOrPut(player.uniqueId) { ConcurrentHashMap() }
        val previous = forPlayer[owner]?.chunks?.keys?.toSet() ?: emptySet()

        val overlay = OwnerOverlay(orderCounter.incrementAndGet())
        chunks.forEach { overlay.chunks[it] = biome }
        forPlayer[owner] = overlay

        // Chunks this owner has just released still need to be repainted by whoever is underneath,
        // or restored, otherwise they keep showing a biome nobody asks for any more.
        refresh(player, previous - chunks)
        refresh(player, chunks)
        return chunks.size
    }

    /** Drops the overlay [owner] holds on [player], restoring whatever is underneath. */
    fun clear(player: Player, owner: String) {
        val forPlayer = overlays[player.uniqueId] ?: return
        val removed = forPlayer.remove(owner) ?: return
        if (forPlayer.isEmpty()) overlays.remove(player.uniqueId)
        refresh(player, removed.chunks.keys.toSet())
    }

    /** Drops every overlay of [player] and restores what the world really contains. */
    fun clear(player: Player) {
        val forPlayer = overlays.remove(player.uniqueId) ?: return
        val chunks = forPlayer.values.flatMap { it.chunks.keys }.toSet()
        refresh(player, chunks)
    }

    fun forget(playerId: UUID) {
        overlays.remove(playerId)
    }

    /** The biome [player] is being shown at this position, or null when no overlay applies. */
    fun overlayAt(player: Player, world: World, blockX: Int, blockZ: Int): NamespacedKey? =
        winnerAt(player.uniqueId, ChunkPos(world.uid, blockX shr 4, blockZ shr 4))

    fun hasOverlay(player: Player): Boolean =
        overlays[player.uniqueId]?.values?.any { it.chunks.isNotEmpty() } == true

    /**
     * The biome shown at [chunk], across every owner. The most recently applied owner wins, so a
     * quest overlay laid on top of an ambient one is what the player actually sees.
     */
    private fun winnerAt(playerId: UUID, chunk: ChunkPos): NamespacedKey? =
        overlays[playerId]
            ?.values
            ?.mapNotNull { overlay -> overlay.chunks[chunk]?.let { overlay.order to it } }
            ?.maxByOrNull { it.first }
            ?.second

    /**
     * Sends [chunks] again with whatever should be visible there now: the winning overlay, or the
     * real world when no overlay covers the chunk any more.
     */
    private fun refresh(player: Player, chunks: Set<ChunkPos>) {
        if (chunks.isEmpty()) return

        val world = player.world
        val relevant = chunks.filter { it.world == world.uid }
        if (relevant.isEmpty()) return

        val byBiome = mutableMapOf<NamespacedKey, MutableSet<Pair<Int, Int>>>()
        val restore = mutableSetOf<Pair<Int, Int>>()

        for (chunk in relevant) {
            val winner = winnerAt(player.uniqueId, chunk)
            if (winner == null) restore += chunk.x to chunk.z
            else byBiome.getOrPut(winner) { mutableSetOf() } += chunk.x to chunk.z
        }

        byBiome.forEach { (biome, positions) ->
            val networkId = CustomBiomeRegistry.injector.networkId(biome) ?: return@forEach
            BiomePacketHelper.sendSingleBiome(player, world, positions, networkId)
        }
        if (restore.isNotEmpty()) BiomePacketHelper.sendRealBiomes(player, world, restore)
    }

    /**
     * Re-applies the overlay right after the server sends a chunk the player has one for.
     * Without this the vanilla chunk data would win as soon as the chunk is resent.
     */
    private class ChunkListener : PacketListenerAbstract(PacketListenerPriority.LOW) {

        override fun onPacketSend(event: PacketSendEvent) {
            if (event.packetType != PacketType.Play.Server.CHUNK_DATA) return

            // `Player?` on the variable: with `getPlayer<Player>()` the elvis is compiled away and
            // a channel without a Bukkit player throws instead of being skipped.
            val player: Player? = event.getPlayer()
            if (player == null) return
            val forPlayer = overlays[player.uniqueId] ?: return
            if (forPlayer.isEmpty()) return

            val column = WrapperPlayServerChunkData(event).column
            val world = player.world
            val biome = winnerAt(player.uniqueId, ChunkPos(world.uid, column.x, column.z)) ?: return
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
