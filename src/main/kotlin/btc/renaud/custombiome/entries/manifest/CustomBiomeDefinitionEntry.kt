package btc.renaud.custombiome.entries.manifest

import btc.renaud.custombiome.model.BiomeColors
import btc.renaud.custombiome.model.BiomeAttributes
import btc.renaud.custombiome.model.CustomBiomeDefinition
import btc.renaud.custombiome.registry.CustomBiomeRegistry
import btc.renaud.custombiome.util.ColorUtils
import btc.renaud.custombiome.util.BiomeResolver
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.TypewriterPaperPlugin
import com.typewritermc.engine.paper.entry.ManifestEntry
import com.typewritermc.engine.paper.events.TypewriterUnloadEvent
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.koin.java.KoinJavaComponent.get
import java.util.Locale

/**
 * Manifest entry for defining a custom biome.
 * 
 * Custom biomes are registered as datapacks and require a server restart
 * for changes to take effect. Once registered, they can be used like
 * vanilla biomes in all biome-related entries and actions.
 */
@Tags("custombiome", "manifest", "definition")
@Entry(
    "custom_biome_definition",
    "Define a custom biome with colors and climate settings",
    Colors.ORANGE,
    icon = "mdi:pine-tree-box"
)
class CustomBiomeDefinitionEntry(
    override val id: String = "",
    override val name: String = "",
    
    // ═══════════════════════════════════════════════════════════════════════════
    // IDENTIFICATION
    // ═══════════════════════════════════════════════════════════════════════════
    @Help("Unique biome ID (lowercase, no spaces, use underscores)")
    val biomeId: String = "my_custom_biome",
    
    @Help("Namespace for the biome key (default: typewriter)")
    val namespace: String = "typewriter",
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BASE BIOME
    // ═══════════════════════════════════════════════════════════════════════════
    @Help("Base vanilla biome to inherit properties from (e.g., 'minecraft:plains', 'minecraft:forest')")
    val baseBiome: String = "minecraft:plains",
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CLIMATE
    // ═══════════════════════════════════════════════════════════════════════════
    @Help("Temperature value (0.0 = cold/snowy, 0.5 = temperate, 2.0 = hot). Leave empty to inherit from base biome.")
    val temperature: String = "",
    
    @Help("Downfall/humidity value (0.0 = dry, 1.0 = wet/rainy). Leave empty to inherit from base biome.")
    val downfall: String = "",
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLORS (Hex format: #RRGGBB)
    // ═══════════════════════════════════════════════════════════════════════════
    @Help("Fog color in hex format (#RRGGBB). Leave empty to inherit from base biome.")
    val fogColor: String = "",
    
    @Help("Water color in hex format (#RRGGBB). Leave empty to inherit from base biome.")
    val waterColor: String = "",
    
    @Help("Underwater fog color in hex format (#RRGGBB). Leave empty to inherit from base biome.")
    val waterFogColor: String = "",
    
    @Help("Sky color in hex format (#RRGGBB). Leave empty to inherit from base biome.")
    val skyColor: String = "",
    
    @Help("Foliage/leaf color in hex format (#RRGGBB). Leave empty to inherit from base biome.")
    val foliageColor: String = "",
    
    @Help("Grass color in hex format (#RRGGBB). Leave empty to inherit from base biome.")
    val grassColor: String = "",
    
    @Help("Dry foliage color for badlands/savanna in hex format (#RRGGBB). Leave empty to inherit from base biome.")
    val dryFoliageColor: String = "",
    
    @Help("Sunrise/sunset sky tint color in hex format (#RRGGBB). Leave empty to inherit from base biome.")
    val sunriseSunsetColor: String = "",
    
    @Help("Cloud color in hex format (#RRGGBB). Leave empty to inherit from base biome.")
    val cloudColor: String = "",
    
    @Help("Skylight color tint in hex format (#RRGGBB). Leave empty to inherit from base biome.")
    val skyLightColor: String = "",

    // ═══════════════════════════════════════════════════════════════════════════
    // VISUAL ATTRIBUTES (1.21.11+)
    // ═══════════════════════════════════════════════════════════════════════════

    @Help("Distance where fog starts")
    val fogStartDistance: Float? = null,
    
    @Help("Distance where fog ends")
    val fogEndDistance: Float? = null,

    @Help("Distance where sky fog ends")
    val skyFogEndDistance: Float? = null,

    @Help("Distance where water fog starts")
    val waterFogStartDistance: Float? = null,

    @Help("Distance where water fog ends")
    val waterFogEndDistance: Float? = null,
    
    @Help("Distance where cloud fog ends")
    val cloudFogEndDistance: Float? = null,

    @Help("Height of the clouds")
    val cloudHeight: Float? = null,

    @Help("Multiplier for sky light brightness")
    val skyLightFactor: Float? = null,

    @Help("Sun angle (0.0 = overhead)")
    val sunAngle: Float? = null,

    @Help("Moon angle")
    val moonAngle: Float? = null,

    @Help("Star angle")
    val starAngle: Float? = null,

    @Help("Star brightness")
    val starBrightness: Float? = null,

    @Help("Moon phase (full_moon, waning_gibbous, etc)")
    val moonPhase: String? = null
) : ManifestEntry, Listener {
    
    companion object {
        private val plugin: JavaPlugin by lazy { 
            JavaPlugin.getPlugin(TypewriterPaperPlugin::class.java) 
        }
    }
    
    fun register() {
        Bukkit.getPluginManager().registerEvents(this, plugin)        
        val registry = get<CustomBiomeRegistry>(CustomBiomeRegistry::class.java)
        
        val normalizedId = biomeId.lowercase(Locale.ENGLISH)
            .replace(' ', '_')
            .replace('-', '_')
        
        val key = NamespacedKey(namespace.lowercase(Locale.ENGLISH), normalizedId)
        
        val definition = toModel(key)
        
        registry.registerDefinition(definition)
    }

    fun toModel(key: NamespacedKey): CustomBiomeDefinition {
        val baseBiomeKey = baseBiome?.let { BiomeResolver.resolveKey(it) } ?: NamespacedKey.minecraft("plains")
        
        // Effects (Standard colors)
        val colors = BiomeColors(
            grass = grassColor?.let { ColorUtils.parseHexColor(it) },
            foliage = foliageColor?.let { ColorUtils.parseHexColor(it) },
            dryFoliage = dryFoliageColor?.let { ColorUtils.parseHexColor(it) },
            water = waterColor?.let { ColorUtils.parseHexColor(it) }
            // Note: Fog/Sky/etc moved to Attributes for 1.21.11+, effectively ignored in 'colors' if we stop using them there
        )

        // Visual Attributes (1.21.11+)
        val attributes = BiomeAttributes(
            sky = skyColor?.let { ColorUtils.parseHexColor(it) },
            fog = fogColor?.let { ColorUtils.parseHexColor(it) },
            waterFog = waterFogColor?.let { ColorUtils.parseHexColor(it) },
            cloud = cloudColor?.let { ColorUtils.parseHexColor(it) },
            skyLight = skyLightColor?.let { ColorUtils.parseHexColor(it) },
            sunriseSunset = sunriseSunsetColor?.let { ColorUtils.parseHexColor(it) },
            
            fogStartDistance = fogStartDistance,
            fogEndDistance = fogEndDistance,
            skyFogEndDistance = skyFogEndDistance,
            waterFogStartDistance = waterFogStartDistance,
            waterFogEndDistance = waterFogEndDistance,
            cloudFogEndDistance = cloudFogEndDistance,
            
            cloudHeight = cloudHeight,
            skyLightFactor = skyLightFactor,
            
            sunAngle = sunAngle,
            moonAngle = moonAngle,
            starAngle = starAngle,
            starBrightness = starBrightness,
            moonPhase = moonPhase
        )

        return CustomBiomeDefinition(
            key = key,
            baseKey = baseBiomeKey,
            colors = colors,
            attributes = attributes,
            temperature = temperature.toDoubleOrNull(),
            downfall = downfall.toDoubleOrNull()
        )
    }    
    
    @EventHandler
    fun onUnload(event: TypewriterUnloadEvent) {
        HandlerList.unregisterAll(this)
        
        val registry = get<CustomBiomeRegistry>(CustomBiomeRegistry::class.java)
        val normalizedId = biomeId.lowercase(Locale.ENGLISH)
            .replace(' ', '_')
            .replace('-', '_')
        val key = NamespacedKey(namespace.lowercase(Locale.ENGLISH), normalizedId)
        registry.unregisterDefinition(key)
    }
}
