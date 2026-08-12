package btcrenaud.custombiome.entries.manifest

import btcrenaud.custombiome.model.BiomeColors
import btcrenaud.custombiome.model.BiomeAttributes
import btcrenaud.custombiome.model.CustomBiomeDefinition
import btcrenaud.custombiome.registry.CustomBiomeRegistry
import btcrenaud.custombiome.util.ColorUtils
import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.ManifestEntry
import btcrenaud.custombiome.injector.BiomeInjectionResult
import btcrenaud.custombiome.util.BiomeKeys
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import org.bukkit.NamespacedKey

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

    @Help("Optional preset providing shared colors and visual attributes. Anything set below wins over it.")
    val preset: Ref<CustomBiomePresetEntry> = emptyRef(),
    
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

    @Help("Multiplier for sky light brightness. Must be between 0.0 and 1.0.")
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
) : ManifestEntry {

    /** Registry key this entry owns, derived once so every caller agrees on it. */
    val key: NamespacedKey get() = BiomeKeys.of(namespace, biomeId)

    fun register(): BiomeInjectionResult = CustomBiomeRegistry.registerDefinition(toModel(key))

    fun toModel(key: NamespacedKey): CustomBiomeDefinition {
        val baseBiomeKey = BiomeResolver.resolveKey(baseBiome) ?: NamespacedKey.minecraft("plains")

        // A value set on this entry always wins; anything left empty falls back to the preset, and
        // only then to the base biome.
        val inheritedColors = preset.get()?.colors() ?: BiomeColors.EMPTY
        val inheritedAttributes = preset.get()?.attributes() ?: BiomeAttributes()

        val colors = BiomeColors(
            grass = ColorUtils.parseHexColor(grassColor) ?: inheritedColors.grass,
            foliage = ColorUtils.parseHexColor(foliageColor) ?: inheritedColors.foliage,
            dryFoliage = ColorUtils.parseHexColor(dryFoliageColor) ?: inheritedColors.dryFoliage,
            water = ColorUtils.parseHexColor(waterColor) ?: inheritedColors.water,
        )

        // Sky, fog and the rest moved to environment attributes in modern versions.
        val attributes = BiomeAttributes(
            sky = ColorUtils.parseHexColor(skyColor) ?: inheritedAttributes.sky,
            fog = ColorUtils.parseHexColor(fogColor) ?: inheritedAttributes.fog,
            waterFog = ColorUtils.parseHexColor(waterFogColor) ?: inheritedAttributes.waterFog,
            cloud = ColorUtils.parseHexColor(cloudColor) ?: inheritedAttributes.cloud,
            skyLight = ColorUtils.parseHexColor(skyLightColor) ?: inheritedAttributes.skyLight,
            sunriseSunset = ColorUtils.parseHexColor(sunriseSunsetColor) ?: inheritedAttributes.sunriseSunset,

            fogStartDistance = fogStartDistance ?: inheritedAttributes.fogStartDistance,
            fogEndDistance = fogEndDistance ?: inheritedAttributes.fogEndDistance,
            skyFogEndDistance = skyFogEndDistance ?: inheritedAttributes.skyFogEndDistance,
            waterFogStartDistance = waterFogStartDistance ?: inheritedAttributes.waterFogStartDistance,
            waterFogEndDistance = waterFogEndDistance ?: inheritedAttributes.waterFogEndDistance,
            cloudFogEndDistance = cloudFogEndDistance ?: inheritedAttributes.cloudFogEndDistance,

            cloudHeight = cloudHeight ?: inheritedAttributes.cloudHeight,
            skyLightFactor = skyLightFactor ?: inheritedAttributes.skyLightFactor,

            sunAngle = sunAngle ?: inheritedAttributes.sunAngle,
            moonAngle = moonAngle ?: inheritedAttributes.moonAngle,
            starAngle = starAngle ?: inheritedAttributes.starAngle,
            starBrightness = starBrightness ?: inheritedAttributes.starBrightness,
            moonPhase = moonPhase ?: inheritedAttributes.moonPhase,
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
}
