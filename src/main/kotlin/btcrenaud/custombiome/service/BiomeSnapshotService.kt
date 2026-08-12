package btcrenaud.custombiome.service

import btcrenaud.custombiome.util.BiomeResolver
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.typewritermc.engine.paper.plugin
import org.bukkit.Bukkit
import org.bukkit.World
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * Records what a region looked like before it was painted, so it can be put back.
 *
 * Snapshots are generated at runtime from world data, which is why they live as plain files rather
 * than as editor-authored artifacts: there is nothing for a content creator to author here, only
 * something for the server to remember.
 */
object BiomeSnapshotService {

    private val logger = LoggerFactory.getLogger(BiomeSnapshotService::class.java)
    private val gson: Gson = GsonBuilder().create()

    private data class Cell(val x: Int, val y: Int, val z: Int, val biome: String)
    private data class Snapshot(val world: String, val cells: List<Cell>)

    private val folder: Path by lazy {
        plugin.dataFolder.toPath().resolve("custombiome").resolve("snapshots")
            .also { Files.createDirectories(it) }
    }

    /** Reads the current biome of every position and stores it under [id]. */
    fun capture(id: String, world: World, positions: Collection<Triple<Int, Int, Int>>): Boolean {
        if (positions.isEmpty()) return false

        return runCatching {
            val cells = positions.map { (x, y, z) ->
                Cell(x, y, z, world.getBiome(x, y, z).key.toString())
            }

            val target = fileFor(id)
            Files.newBufferedWriter(target, StandardCharsets.UTF_8).use {
                gson.toJson(Snapshot(world.uid.toString(), cells), it)
            }
            logger.info("Captured biome snapshot '{}' with {} cell(s).", id, cells.size)
            true
        }.getOrElse { error ->
            logger.error("Failed to capture biome snapshot '$id'", error)
            false
        }
    }

    /**
     * Puts back every biome recorded under [id]. Returns the number of cells restored, or null
     * when the snapshot does not exist.
     */
    fun restore(id: String): Int? {
        val file = fileFor(id)
        if (!Files.exists(file)) return null

        return runCatching {
            val snapshot = Files.newBufferedReader(file, StandardCharsets.UTF_8).use {
                gson.fromJson(it, Snapshot::class.java)
            }

            val world = Bukkit.getWorld(UUID.fromString(snapshot.world)) ?: run {
                logger.warn("Snapshot '{}' refers to a world that is no longer loaded.", id)
                return null
            }

            // Group by biome so each group is one painter dispatch instead of one per cell.
            snapshot.cells
                .groupBy { it.biome }
                .forEach { (key, cells) ->
                    val biome = BiomeResolver.resolve(key) ?: return@forEach
                    BiomePainter.paintPositions(world, biome, cells.map { Triple(it.x, it.y, it.z) })
                }

            snapshot.cells.size
        }.getOrElse { error ->
            logger.error("Failed to restore biome snapshot '$id'", error)
            null
        }
    }

    fun exists(id: String): Boolean = Files.exists(fileFor(id))

    fun delete(id: String): Boolean = runCatching { Files.deleteIfExists(fileFor(id)) }.getOrDefault(false)

    /** Snapshot ids come from entries, so they are sanitised before touching the filesystem. */
    private fun fileFor(id: String): Path {
        val safe = id.trim().lowercase()
            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .ifEmpty { "default" }
        return folder.resolve("$safe.json")
    }
}
