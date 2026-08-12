package btcrenaud.custombiome.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.typewritermc.engine.paper.plugin
import btcrenaud.custombiome.util.Scheduling
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which biomes each player has set foot in.
 *
 * Discovery is per player and has to outlive a session, so it is written to disk. Writes are
 * debounced and pushed off the calling thread: this is fed from the movement listener, which is
 * the hottest path on the server.
 */
object BiomeDiscoveryService {

    private val logger = LoggerFactory.getLogger(BiomeDiscoveryService::class.java)
    private val gson = Gson()

    private val discovered = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val dirty = ConcurrentHashMap.newKeySet<UUID>()

    private val folder: Path by lazy {
        plugin.dataFolder.toPath().resolve("custombiome").resolve("discovery")
            .also { Files.createDirectories(it) }
    }

    /** Records [biome] for [player]. Returns true when it was a first-time discovery. */
    fun discover(player: Player, biome: NamespacedKey): Boolean {
        val set = discovered.getOrPut(player.uniqueId) { load(player.uniqueId) }
        if (!set.add(biome.toString())) return false

        dirty += player.uniqueId
        scheduleSave(player.uniqueId)
        return true
    }

    fun hasDiscovered(player: Player, biome: NamespacedKey): Boolean =
        discovered.getOrPut(player.uniqueId) { load(player.uniqueId) }.contains(biome.toString())

    fun count(player: Player): Int =
        discovered.getOrPut(player.uniqueId) { load(player.uniqueId) }.size

    fun all(player: Player): Set<String> =
        discovered.getOrPut(player.uniqueId) { load(player.uniqueId) }.toSet()

    fun unload(playerId: UUID) {
        save(playerId)
        discovered.remove(playerId)
    }

    fun clear() {
        discovered.keys.forEach { save(it) }
        discovered.clear()
    }

    private fun scheduleSave(playerId: UUID) {
        Scheduling.runAsync { save(playerId) }
    }

    private fun save(playerId: UUID) {
        if (!dirty.remove(playerId)) return
        val set = discovered[playerId] ?: return

        runCatching {
            Files.newBufferedWriter(fileFor(playerId), StandardCharsets.UTF_8).use {
                gson.toJson(set, it)
            }
        }.onFailure { logger.error("Failed to save biome discovery for $playerId", it) }
    }

    private fun load(playerId: UUID): MutableSet<String> {
        val file = fileFor(playerId)
        if (!Files.exists(file)) return ConcurrentHashMap.newKeySet()

        return runCatching {
            Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<Set<String>>() {}.type
                val stored: Set<String>? = gson.fromJson(reader, type)
                ConcurrentHashMap.newKeySet<String>().apply { stored?.let { addAll(it) } }
            }
        }.getOrElse {
            logger.warn("Could not read biome discovery for {}, starting empty.", playerId)
            ConcurrentHashMap.newKeySet()
        }
    }

    private fun fileFor(playerId: UUID): Path = folder.resolve("$playerId.json")
}
