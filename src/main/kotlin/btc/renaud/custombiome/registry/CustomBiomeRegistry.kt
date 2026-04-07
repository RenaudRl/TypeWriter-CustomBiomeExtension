package btc.renaud.custombiome.registry

import btc.renaud.custombiome.model.BiomeColors
import btc.renaud.custombiome.model.CustomBiomeDefinition
import btc.renaud.custombiome.util.BiomePacketHelper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Biome
import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * Registry for custom biome definitions.
 * Manages biome registration and datapack generation.
 */
class CustomBiomeRegistry : KoinComponent {
    
    private val logger = LoggerFactory.getLogger(CustomBiomeRegistry::class.java)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    
    private val definitions = ConcurrentHashMap<NamespacedKey, CustomBiomeDefinition>()
    private val baseJsonCache = ConcurrentHashMap<NamespacedKey, JsonObject>()
    
    private var datapackFolder: Path? = null
    
    companion object {
        private const val DATAPACK_ID = "typewriter_custom_biomes"
        // Pack format for MC 1.21.x datapacks
        private const val PACK_FORMAT = 48
        private val DEFAULT_BASE_BIOME: NamespacedKey = NamespacedKey.minecraft("plains")
    }
    
    /**
     * Initialize the registry with the datapack folder path.
     */
    fun initialize(dataDirectory: Path) {
        // We write to the primary world's datapack folder to ensure Minecraft discovers it
        val world = Bukkit.getWorlds().firstOrNull()
        val worldFolder = world?.worldFolder ?: java.io.File("world")
        
        datapackFolder = worldFolder.toPath().resolve("datapacks").resolve(DATAPACK_ID)
        
        Bukkit.getLogger().info("[CustomBiome] Registry initialized at $datapackFolder")
    }
    
    /**
     * Register a biome definition.
     * Note: Requires server restart for the biome to be available in world generation.
     */
    fun registerDefinition(definition: CustomBiomeDefinition) {
        definitions[definition.key] = definition
        // Schedule a sync to disk (deferred to handle batch loading)
        scheduleSync()
    }
    
    /**
     * Unregister a biome definition.
     */
    fun unregisterDefinition(key: NamespacedKey) {
        definitions.remove(key)
        scheduleSync()
    }
    
    /**
     * Get a biome definition by key.
     */
    fun getDefinition(key: NamespacedKey): CustomBiomeDefinition? = definitions[key]
    
    /**
     * Get all registered definitions.
     */
    fun allDefinitions(): List<CustomBiomeDefinition> = definitions.values.toList()
    
    /**
     * Get all registered custom biomes from Bukkit registry.
     */
    fun allBiomes(): List<Biome> {
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
        return definitions.keys.mapNotNull { registry.get(it) }
    }
    
    /**
     * Resolve a biome from the registry.
     */
    fun resolveBiome(key: NamespacedKey): Biome? {
        return RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.BIOME)
            .get(key)
    }
    
    /**
     * Get the number of registered custom biomes.
     */
    fun count(): Int = definitions.size
    
    /**
     * Apply a biome to a location with optional radius.
     * 
     * @return Number of columns affected
     */
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
        
        // Send biome refresh packets
        if (changedChunks.isNotEmpty()) {
            BiomePacketHelper.sendBiomePackets(world, changedChunks)
        }
        
        return columnsAffected
    }
    
    private var syncJobStarted = false
    
    /**
     * Schedule a datapack synchronization.
     * Uses a small delay to avoid excessive writes during batch loading.
     */
    fun scheduleSync() {
        if (syncJobStarted) return
        syncJobStarted = true
        
        // Run after 1 second to ensure all initial entries are loaded
        Bukkit.getScheduler().runTaskLater(com.typewritermc.engine.paper.plugin, Runnable {
            prepareDatapack()
            syncJobStarted = false
        }, 20L)
    }
    
    /**
     * Prepare the datapack for server loading.
     * Should be called during bootstrap phase (for early discovery) or at runtime.
     */
    fun prepareDatapack() {
        val folder = datapackFolder ?: return
        
        runCatching {
            resetDirectory(folder)
            writePackMetadata(folder)
            
            if (definitions.isEmpty()) {
                logger.warn("Registry is empty, skipping datapack generation to prevent data loss.")
                return
            }
            
            definitions.values.forEach { definition ->
                val json = createBiomeJson(definition)
                val target = folder
                    .resolve("data")
                    .resolve(definition.key.namespace)
                    .resolve("worldgen/biome")
                    .resolve("${definition.key.key}.json")
                
                Files.createDirectories(target.parent)
                Files.newBufferedWriter(target, StandardCharsets.UTF_8).use { writer ->
                    gson.toJson(json, writer)
                }
            }
            
            logger.info("Prepared datapack with {} custom biome definitions", definitions.size)
        }.onFailure { error ->
            logger.error("Failed to prepare custom biome datapack", error)
        }
    }
    
    /**
     * Reload the datapack after configuration changes.
     */
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
        
        // ═══════════════════════════════════════════════════════════════════════════
        // EFFECTS SECTION - Standard biome visual effects
        // ═══════════════════════════════════════════════════════════════════════════
        
        // Water color (required)
        definition.colors.water?.let { effects.addProperty("water_color", it) }
        
        // Grass color with modifier
        definition.colors.grass?.let {
            effects.addProperty("grass_color", it)
            effects.addProperty("grass_color_modifier", "none")
        }
        
        // Foliage color
        definition.colors.foliage?.let { effects.addProperty("foliage_color", it) }
        
        // Dry foliage color (MC 1.21.5+)
        definition.colors.dryFoliage?.let { effects.addProperty("dry_foliage_color", it) }
        
        // ═══════════════════════════════════════════════════════════════════════════
        // ATTRIBUTES SECTION - Environment visual attributes (MC 1.21.11+)
        // Keys are namespaced, e.g., "visual/sky_color" or sometimes "minecraft:visual/sky_color"
        // Based on analysis, we use the "visual/" prefix as requested/documented.
        // ═══════════════════════════════════════════════════════════════════════════
        
        // ═══════════════════════════════════════════════════════════════════════════
        // ATTRIBUTES SECTION - Environment visual attributes (MC 1.21.11+)
        // Keys are namespaced, e.g., "visual/sky_color".
        // We must remove potential legacy keys (e.g. "sky_color") to avoid "Duplicate entry"
        // errors if they map to the same internal attribute.
        // ═══════════════════════════════════════════════════════════════════════════
        
        // Helper to safely set visual attribute and remove potential conflicts
        fun setAttribute(visualKey: String, legacyKey: String?, value: Any) {
            // Robust removal of existing keys to prevent "Duplicate entry" errors
            // Use iterator to safely remove keys while iterating
            val iterator = attributes.entrySet().iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val key = entry.key
                
                // Check for matches (exact, namespaced, or legacy)
                // We check if the key is the visual key, the legacy key, or if it is namespaced with minecraft:
                val isMatch = key == visualKey || 
                              key == "minecraft:$visualKey" ||
                              (legacyKey != null && (key == legacyKey || key == "minecraft:$legacyKey"))
                
                if (isMatch) {
                    iterator.remove()
                }
            }
            
            // Add property based on type
            when (value) {
                is Number -> attributes.addProperty(visualKey, value)
                is String -> attributes.addProperty(visualKey, value)
                is Boolean -> attributes.addProperty(visualKey, value)
            }
        }

        // Colors
        definition.attributes.sky?.let { setAttribute("visual/sky_color", "sky_color", it) }
        definition.attributes.fog?.let { setAttribute("visual/fog_color", "fog_color", it) }
        definition.attributes.waterFog?.let { setAttribute("visual/water_fog_color", "water_fog_color", it) }
        definition.attributes.cloud?.let { setAttribute("visual/cloud_color", "cloud_color", it) }
        definition.attributes.skyLight?.let { setAttribute("visual/sky_light_color", "sky_light_color", it) }
        definition.attributes.sunriseSunset?.let { setAttribute("visual/sunrise_sunset_color", "sunrise_sunset_color", it) }

        // Fog Distances
        definition.attributes.fogStartDistance?.let { setAttribute("visual/fog_start_distance", "fog_start_distance", it) }
        definition.attributes.fogEndDistance?.let { setAttribute("visual/fog_end_distance", "fog_end_distance", it) }
        definition.attributes.skyFogEndDistance?.let { setAttribute("visual/sky_fog_end_distance", "sky_fog_end_distance", it) }
        definition.attributes.waterFogStartDistance?.let { setAttribute("visual/water_fog_start_distance", "water_fog_start_distance", it) }
        definition.attributes.waterFogEndDistance?.let { setAttribute("visual/water_fog_end_distance", "water_fog_end_distance", it) }
        definition.attributes.cloudFogEndDistance?.let { setAttribute("visual/cloud_fog_end_distance", "cloud_fog_end_distance", it) }

        // Cloud & Light
        definition.attributes.cloudHeight?.let { setAttribute("visual/cloud_height", "cloud_height", it) }
        definition.attributes.skyLightFactor?.let { setAttribute("visual/sky_light_factor", "sky_light_factor", it) }

        // Celestial
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
