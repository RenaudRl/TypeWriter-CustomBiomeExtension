package btcrenaud.custombiome.registry

import btcrenaud.custombiome.injector.BiomeInjectionResult
import btcrenaud.custombiome.injector.BiomeInjector
import btcrenaud.custombiome.injector.ReflectionBiomeInjector
import btcrenaud.custombiome.model.CustomBiomeDefinition
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single source of truth for custom biomes.
 *
 * A definition is registered in two complementary ways:
 *  - injected into the live registry, so it is usable immediately without a restart;
 *  - written to a generated datapack, so it survives a restart and exists for world generation.
 *
 * When live injection is unavailable the datapack alone still carries the biome, and the reason is
 * logged once rather than swallowed.
 */
object CustomBiomeRegistry {

    private val logger = LoggerFactory.getLogger(CustomBiomeRegistry::class.java)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private val definitions = ConcurrentHashMap<NamespacedKey, CustomBiomeDefinition>()
    private val pendingRestart = ConcurrentHashMap.newKeySet<NamespacedKey>()

    private const val DATAPACK_ID = "typewriter_custom_biomes"

    /** Format used only if the server refuses to tell us its own; see [BiomeInjector.dataPackFormat]. */
    private const val FALLBACK_PACK_FORMAT = 88

    private var datapackFolder: Path? = null
    private val syncScheduled = AtomicBoolean(false)

    val injector: BiomeInjector by lazy { ReflectionBiomeInjector.create() }

    fun initialize(worldFolder: Path) {
        datapackFolder = levelRoot(worldFolder).resolve("datapacks").resolve(DATAPACK_ID)
        logger.info("Custom biome datapack target: {} — strategy: {}", datapackFolder, injector.describe)
    }

    /**
     * The server only reads datapacks from the level root, but a world folder may point at a
     * dimension subfolder (`<level>/dimensions/minecraft/overworld`) depending on the platform.
     * Climbing back out is what keeps the generated pack somewhere the server actually loads.
     */
    private fun levelRoot(worldFolder: Path): Path {
        val dimensionSegments = setOf("overworld", "the_nether", "the_end", "minecraft", "dimensions")
        var current = worldFolder.toAbsolutePath().normalize()
        while (current.fileName?.toString()?.lowercase() in dimensionSegments) {
            current = current.parent ?: return current
        }
        return current
    }

    /**
     * Registers [definition] and makes it usable straight away when the platform allows it.
     * Returns the outcome so callers can tell an operator what actually happened.
     */
    fun registerDefinition(definition: CustomBiomeDefinition): BiomeInjectionResult {
        definitions[definition.key] = definition

        val result = injector.inject(definition)
        when (result) {
            is BiomeInjectionResult.Injected,
            is BiomeInjectionResult.AlreadyPresent -> pendingRestart.remove(definition.key)

            is BiomeInjectionResult.Failed -> {
                pendingRestart += definition.key
                logger.warn(
                    "Biome {} could not be registered live ({}). It will exist after a restart via the datapack.",
                    definition.key, result.reason
                )
            }
        }

        scheduleSync()
        return result
    }

    fun unregisterDefinition(key: NamespacedKey) {
        // The live registry cannot drop an entry without corrupting holders that reference it, so a
        // removed biome disappears from the datapack now and from the registry on the next restart.
        if (definitions.remove(key) != null) scheduleSync()
    }

    fun getDefinition(key: NamespacedKey): CustomBiomeDefinition? = definitions[key]

    fun allDefinitions(): List<CustomBiomeDefinition> = definitions.values.toList()

    fun count(): Int = definitions.size

    /** True when the biome exists only on paper and needs a restart to become real. */
    fun awaitingRestart(key: NamespacedKey): Boolean = key in pendingRestart

    fun resolveBiome(key: NamespacedKey): Biome? =
        RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(key)

    fun allBiomes(): List<Biome> {
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
        return definitions.keys.mapNotNull { registry.get(it) }
    }

    /**
     * Coalesces the bursts of registrations that happen when Typewriter loads a book, so the
     * datapack is written once instead of once per definition.
     */
    private fun scheduleSync() {
        if (!syncScheduled.compareAndSet(false, true)) return

        Bukkit.getGlobalRegionScheduler().runDelayed(com.typewritermc.engine.paper.plugin, { _ ->
            syncScheduled.set(false)
            writeDatapack()
        }, 20L)
    }

    /**
     * Writes every definition to the generated datapack. Each biome is encoded from the server's
     * own codec after inheriting its base biome, which is why no base JSON is bundled with the jar.
     */
    fun writeDatapack() {
        val folder = datapackFolder ?: return

        runCatching {
            if (definitions.isEmpty()) {
                // An empty registry means "no custom biomes", not "wipe the pack mid-load".
                logger.debug("No custom biome definitions; leaving the datapack untouched.")
                return
            }

            val encoded = definitions.values.mapNotNull { definition ->
                val json = injector.encodeToJson(definition)
                if (json == null) {
                    logger.warn("Skipping {} in the datapack: the server refused to encode it.", definition.key)
                    null
                } else {
                    definition to json
                }
            }

            if (encoded.isEmpty()) {
                logger.warn("No custom biome could be encoded; keeping the previous datapack rather than emptying it.")
                return
            }

            resetDirectory(folder)
            writePackMetadata(folder)

            encoded.forEach { (definition, json) ->
                val target = folder
                    .resolve("data")
                    .resolve(definition.key.namespace)
                    .resolve("worldgen/biome")
                    .resolve("${definition.key.key}.json")

                Files.createDirectories(target.parent)
                Files.newBufferedWriter(target, StandardCharsets.UTF_8).use { gson.toJson(json, it) }
            }

            logger.info("Wrote {} custom biome definition(s) to the datapack.", encoded.size)
        }.onFailure { error ->
            logger.error("Failed to write the custom biome datapack", error)
        }
    }

    private fun writePackMetadata(directory: Path) {
        val format = injector.dataPackFormat() ?: FALLBACK_PACK_FORMAT.also {
            logger.warn("Could not read the server data pack format; falling back to {}.", it)
        }

        val pack = JsonObject().apply {
            add("pack", JsonObject().apply {
                addProperty("description", "Custom biomes generated by the Typewriter CustomBiome extension")
                addProperty("pack_format", format)
            })
        }

        Files.createDirectories(directory)
        Files.newBufferedWriter(directory.resolve("pack.mcmeta"), StandardCharsets.UTF_8).use {
            gson.toJson(pack, it)
        }
    }

    private fun resetDirectory(path: Path) {
        if (Files.exists(path)) {
            Files.walk(path).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
        Files.createDirectories(path)
    }
}
