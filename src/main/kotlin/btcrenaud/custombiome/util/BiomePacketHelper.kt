package btcrenaud.custombiome.util

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk
import org.bukkit.World
import org.bukkit.entity.Player
import org.slf4j.LoggerFactory

/**
 * Helper for sending biome update packets to players using PacketEvents.
 */
object BiomePacketHelper {
    
    private val logger = LoggerFactory.getLogger(BiomePacketHelper::class.java)
    
    /**
     * Send biome update packets to all players in the world for the specified chunks.
     * Uses chunk unload/reload to force client biome refresh.
     * 
     * @param world The world containing the chunks
     * @param chunks Set of (chunkX, chunkZ) pairs to refresh
     */
    fun sendBiomePackets(world: World, chunks: Set<Pair<Int, Int>>) {
        if (chunks.isEmpty()) return
        
        for (player in world.players) {
            refreshChunksForPlayer(player, chunks)
        }
    }
    
    /**
     * Send biome update packets to a specific player for chunks around their location.
     * 
     * @param player The player to send packets to
     * @param radius Chunk radius around the player
     */
    fun refreshBiomesForPlayer(player: Player, radius: Int = 5) {
        val loc = player.location
        val centerX = loc.blockX shr 4
        val centerZ = loc.blockZ shr 4
        
        val chunks = mutableSetOf<Pair<Int, Int>>()
        for (cx in (centerX - radius)..(centerX + radius)) {
            for (cz in (centerZ - radius)..(centerZ + radius)) {
                chunks.add(cx to cz)
            }
        }
        
        refreshChunksForPlayer(player, chunks)
    }
    
    /**
     * Send biome update packets for chunks in a radius around a location.
     * 
     * @param world The world
     * @param centerX Block X coordinate
     * @param centerZ Block Z coordinate
     * @param blockRadius Radius in blocks
     */
    fun refreshBiomesInRadius(world: World, centerX: Int, centerZ: Int, blockRadius: Int) {
        val chunkRadius = (blockRadius shr 4) + 1
        val chunkCenterX = centerX shr 4
        val chunkCenterZ = centerZ shr 4
        
        val affectedChunks = mutableSetOf<Pair<Int, Int>>()
        for (cx in (chunkCenterX - chunkRadius)..(chunkCenterX + chunkRadius)) {
            for (cz in (chunkCenterZ - chunkRadius)..(chunkCenterZ + chunkRadius)) {
                affectedChunks.add(cx to cz)
            }
        }
        
        // Send to players who can see these chunks
        for (player in world.players) {
            val playerChunkX = player.location.blockX shr 4
            val playerChunkZ = player.location.blockZ shr 4
            val viewDistance = player.viewDistance
            
            val visibleChunks = affectedChunks.filter { (cx, cz) ->
                kotlin.math.abs(cx - playerChunkX) <= viewDistance &&
                kotlin.math.abs(cz - playerChunkZ) <= viewDistance
            }.toSet()
            
            if (visibleChunks.isNotEmpty()) {
                refreshChunksForPlayer(player, visibleChunks)
            }
        }
    }
    
    /**
     * Force chunk refresh for a player by sending unload packets.
     * The client will request the chunks again, receiving updated biome data.
     */
    private fun refreshChunksForPlayer(player: Player, chunks: Set<Pair<Int, Int>>) {
        runCatching {
            val manager = PacketEvents.getAPI().playerManager
            for ((cx, cz) in chunks) {
                val unloadPacket = WrapperPlayServerUnloadChunk(cx, cz)
                manager.sendPacket(player, unloadPacket)
            }
        }.onFailure { error ->
            logger.warn("Failed to send biome refresh packets to ${player.name}: ${error.message}")
        }
    }
}
