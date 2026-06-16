package btcrenaud.custombiome.registry

import btcrenaud.custombiome.model.CustomBiomeDefinition
import btcrenaud.custombiome.util.BiomePacketHelper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.typewritermc.core.extension.annotations.Singleton
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Biome
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

@Singleton
class CustomBiomeRegistry {

    private val logger = LoggerFactory.getLogger(CustomBiomeRegistry::class.java)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private val definitions = ConcurrentHashMap<NamespacedKey, CustomBiomeDefinition>()
    private val baseJsonCache = ConcurrentHashMap<NamespacedKey, JsonObject>()

    private var datapackFolder: Path? = null

    private val syncJobStarted = AtomicBoolean(false)

    private val DATAPACK_ID = "typewriter_custom_biomes"
    private val PACK_FORMAT = 48
    private val DEFAULT_BASE_BIOME: NamespacedKey = NamespacedKey.minecraft("plains")

    fun initialize(dataDirectory: Path) {
        val world = Bukkit.getWorlds().firstOrNull()
        val worldFolder = world?.worldFolder ?: java.io.File("world")
        datapackFolder = worldFolder.toPath().resolve("datapacks").resolve(DATAPACK_ID)
        logger.info("[CustomBiome] Registry initialized at $datapackFolder")
    }

    fun registerDefinition(definition: CustomBiomeDefinition) {
        definitions[definition.key] = definition
        scheduleSync()
    }

    fun unregisterDefinition(key: NamespacedKey) {
        definitions.remove(key)
        scheduleSync()
    }

    fun getDefinition(key: NamespacedKey): CustomBiomeDefinition? = definitions[key]

    fun allDefinitions(): List<CustomBiomeDefinition> = definitions.values.toList()

    fun allBiomes(): List<Biome> {
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
        return definitions.keys.mapNotNull { registry.get(it) }
    }

    fun resolveBiome(key: NamespacedKey): Biome? {
        return RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.BIOME)
            .get(key)
    }

    fun count(): Int = definitions.size

    fun applyBiome(location: Location, biome: Biome, radius: Int = 0): Int {
        val world = location.world ?: return 0
        val blockRadius = max(0, radius)
        val minY = world.minHeight
        val maxY = world.maxHeight
        val centerX = location.blockX
        val centerZ = location.blockZ
        val minX = centerX - blockRadius
        val maxX = centerX + blockRadius
        val minZ = centerZ - blockRadius
        val maxZ = centerZ + blockRadius
        val radiusSquared = blockRadius.toLong() * blockRadius.toLong()

        val changedChunks = mutableSetOf<Pair<Int, Int>>()
        var columnsAffected = 0

        for (x in minX..maxX) {
            val dx = (x - centerX).toLong()
            for (z in minZ..maxZ) {
                val dz = (z - centerZ).toLong()
                if (blockRadius > 0 && dx * dx + dz * dz > radiusSquared) continue

                for (y in minY until maxY) {
                    world.setBiome(x, y, z, biome)
                }
                columnsAffected++
                changedChunks += (x shr 4) to (z shr 4)
            }
        }

        if (changedChunks.isNotEmpty()) {
            BiomePacketHelper.sendBiomePackets(world, changedChunks)
        }

        return columnsAffected
    }

    fun scheduleSync() {
        if (!syncJobStarted.compareAndSet(false, true)) return

        Bukkit.getGlobalRegionScheduler().runDelayed(com.typewritermc.engine.paper.plugin, { _ ->
            prepareDatapack()
            syncJobStarted.set(false)
        }, 20L)
    }

    fun prepareDatapack() {
        val folder = datapackFolder ?: return

        runCatching {
            if (definitions.isEmpty()) {
                logger.warn("Registry is empty, skipping datapack generation to prevent data loss.")
                return
            }

            // Write to a temp directory first, then atomically replace the target
            val tempDir = folder.resolveSibling("${folder.fileName}_tmp_${System.currentTimeMillis()}")
            Files.createDirectories(tempDir)

            try {
                writePackMetadata(tempDir)

                definitions.values.forEach { definition ->
                    val json = createBiomeJson(definition)
                    val target = tempDir
                        .resolve("data")
                        .resolve(definition.key.namespace)
                        .resolve("worldgen/biome")
                        .resolve("${definition.key.key}.json")

                    Files.createDirectories(target.parent)
                    Files.newBufferedWriter(target, StandardCharsets.UTF_8).use { writer ->
                        gson.toJson(json, writer)
                    }
                }

                // Atomic replacement: remove old, rename temp to target
                resetDirectory(folder)
                Files.move(tempDir, folder)
                logger.info("Prepared datapack with {} custom biome definitions", definitions.size)
            } catch (e: Exception) {
                // Clean up temp directory on failure
                runCatching { resetDirectory(tempDir) }
                throw e
            }
        }.onFailure { error ->
            logger.error("Failed to prepare custom biome datapack", error)
        }
    }

    fun reload() {
        prepareDatapack()
        Bukkit.reloadData()
        logger.info("Reloaded custom biome datapack with {} entries", definitions.size)
    }

    private fun createBiomeJson(definition: CustomBiomeDefinition): JsonObject {
        val baseKey = definition.baseKey ?: DEFAULT_BASE_BIOME
        val baseJson = loadBaseBiomeJson(baseKey)
        val root = if (baseJson.entrySet().isEmpty() && baseKey != DEFAULT_BASE_BIOME) {
            loadBaseBiomeJson(DEFAULT_BASE_BIOME).deepCopy()
        } else {
            baseJson.deepCopy()
        }

        definition.temperature?.let { root.addProperty("temperature", it) }
        definition.downfall?.let { root.addProperty("downfall", it) }

        val effects = root.getAsJsonObject("effects") ?: JsonObject().also { root.add("effects", it) }
        val attributes = root.getAsJsonObject("attributes") ?: JsonObject().also { root.add("attributes", it) }

        definition.colors.water?.let { effects.addProperty("water_color", it) }
        definition.colors.grass?.let {
            effects.addProperty("grass_color", it)
            effects.addProperty("grass_color_modifier", "none")
        }
        definition.colors.foliage?.let { effects.addProperty("foliage_color", it) }
        definition.colors.dryFoliage?.let { effects.addProperty("dry_foliage_color", it) }

        fun setAttribute(visualKey: String, legacyKey: String?, value: Any) {
            val iterator = attributes.entrySet().iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val key = entry.key
                val isMatch = key == visualKey ||
                              key == "minecraft:$visualKey" ||
                              (legacyKey != null && (key == legacyKey || key == "minecraft:$legacyKey"))
                if (isMatch) {
                    iterator.remove()
                }
            }
            when (value) {
                is Number -> attributes.addProperty(visualKey, value)
                is String -> attributes.addProperty(visualKey, value)
                is Boolean -> attributes.addProperty(visualKey, value)
            }
        }

        definition.attributes.sky?.let { setAttribute("visual/sky_color", "sky_color", it) }
        definition.attributes.fog?.let { setAttribute("visual/fog_color", "fog_color", it) }
        definition.attributes.waterFog?.let { setAttribute("visual/water_fog_color", "water_fog_color", it) }
        definition.attributes.cloud?.let { setAttribute("visual/cloud_color", "cloud_color", it) }
        definition.attributes.skyLight?.let { setAttribute("visual/sky_light_color", "sky_light_color", it) }
        definition.attributes.sunriseSunset?.let { setAttribute("visual/sunrise_sunset_color", "sunrise_sunset_color", it) }

        definition.attributes.fogStartDistance?.let { setAttribute("visual/fog_start_distance", "fog_start_distance", it) }
        definition.attributes.fogEndDistance?.let { setAttribute("visual/fog_end_distance", "fog_end_distance", it) }
        definition.attributes.skyFogEndDistance?.let { setAttribute("visual/sky_fog_end_distance", "sky_fog_end_distance", it) }
        definition.attributes.waterFogStartDistance?.let { setAttribute("visual/water_fog_start_distance", "water_fog_start_distance", it) }
        definition.attributes.waterFogEndDistance?.let { setAttribute("visual/water_fog_end_distance", "water_fog_end_distance", it) }
        definition.attributes.cloudFogEndDistance?.let { setAttribute("visual/cloud_fog_end_distance", "cloud_fog_end_distance", it) }

        definition.attributes.cloudHeight?.let { setAttribute("visual/cloud_height", "cloud_height", it) }
        definition.attributes.skyLightFactor?.let { setAttribute("visual/sky_light_factor", "sky_light_factor", it) }

        definition.attributes.sunAngle?.let { setAttribute("visual/sun_angle", "sun_angle", it) }
        definition.attributes.moonAngle?.let { setAttribute("visual/moon_angle", "moon_angle", it) }
        definition.attributes.starAngle?.let { setAttribute("visual/star_angle", "star_angle", it) }
        definition.attributes.starBrightness?.let { setAttribute("visual/star_brightness", "star_brightness", it) }
        definition.attributes.moonPhase?.let { setAttribute("visual/moon_phase", "moon_phase", it) }

        return root
    }

    private fun loadBaseBiomeJson(key: NamespacedKey): JsonObject {
        return baseJsonCache.computeIfAbsent(key) { namespacedKey ->
            val resourcePath = "data/${namespacedKey.namespace}/worldgen/biome/${namespacedKey.key}.json"
            val inputStream = javaClass.classLoader?.getResourceAsStream(resourcePath)

            if (inputStream == null) {
                logger.warn("Could not locate base biome definition for {}", namespacedKey)
                return@computeIfAbsent JsonObject()
            }

            inputStream.use { stream ->
                InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
                    val parsed = JsonParser.parseReader(reader)
                    if (!parsed.isJsonObject) {
                        logger.warn("Base biome definition {} is not a JSON object", namespacedKey)
                        JsonObject()
                    } else {
                        parsed.asJsonObject
                    }
                }
            }
        }
    }

    private fun writePackMetadata(directory: Path) {
        val pack = JsonObject().apply {
            add("pack", JsonObject().apply {
                addProperty("description", "Custom biomes generated by TypeWriter CustomBiome Extension")
                addProperty("pack_format", PACK_FORMAT)
            })
        }
        val metaFile = directory.resolve("pack.mcmeta")
        Files.newBufferedWriter(metaFile, StandardCharsets.UTF_8).use { writer ->
            gson.toJson(pack, writer)
        }
    }

    private fun resetDirectory(path: Path) {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
        Files.createDirectories(path)
    }
}
