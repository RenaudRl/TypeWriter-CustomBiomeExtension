package btcrenaud.custombiome.entries.manifest

import btcrenaud.custombiome.model.BiomeAttributes
import btcrenaud.custombiome.model.BiomeColors
import btcrenaud.custombiome.util.ColorUtils
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.ManifestEntry

/**
 * A reusable palette shared by several biomes.
 *
 * Without this, a family of biomes that share an atmosphere — the same fog, the same sky, the same
 * star brightness — has to repeat every value in every definition, and a change means editing them
 * one by one. A definition points at a preset and overrides only what differs.
 */
@Tags("custombiome", "manifest", "preset")
@Entry(
    "custom_biome_preset",
    "A reusable set of colors and visual attributes shared by several biomes",
    Colors.ORANGE,
    icon = "mdi:palette-swatch"
)
class CustomBiomePresetEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("Fog color in hex format (#RRGGBB). Leave empty to not set it.")
    val fogColor: String = "",

    @Help("Water color in hex format (#RRGGBB). Leave empty to not set it.")
    val waterColor: String = "",

    @Help("Underwater fog color in hex format (#RRGGBB). Leave empty to not set it.")
    val waterFogColor: String = "",

    @Help("Sky color in hex format (#RRGGBB). Leave empty to not set it.")
    val skyColor: String = "",

    @Help("Foliage/leaf color in hex format (#RRGGBB). Leave empty to not set it.")
    val foliageColor: String = "",

    @Help("Grass color in hex format (#RRGGBB). Leave empty to not set it.")
    val grassColor: String = "",

    @Help("Dry foliage color in hex format (#RRGGBB). Leave empty to not set it.")
    val dryFoliageColor: String = "",

    @Help("Sunrise/sunset sky tint color in hex format (#RRGGBB). Leave empty to not set it.")
    val sunriseSunsetColor: String = "",

    @Help("Cloud color in hex format (#RRGGBB). Leave empty to not set it.")
    val cloudColor: String = "",

    @Help("Skylight color tint in hex format (#RRGGBB). Leave empty to not set it.")
    val skyLightColor: String = "",

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
    val moonPhase: String? = null,

) : ManifestEntry {

    fun colors(): BiomeColors = BiomeColors(
        grass = ColorUtils.parseHexColor(grassColor),
        foliage = ColorUtils.parseHexColor(foliageColor),
        dryFoliage = ColorUtils.parseHexColor(dryFoliageColor),
        water = ColorUtils.parseHexColor(waterColor),
    )

    fun attributes(): BiomeAttributes = BiomeAttributes(
        sky = ColorUtils.parseHexColor(skyColor),
        fog = ColorUtils.parseHexColor(fogColor),
        waterFog = ColorUtils.parseHexColor(waterFogColor),
        cloud = ColorUtils.parseHexColor(cloudColor),
        skyLight = ColorUtils.parseHexColor(skyLightColor),
        sunriseSunset = ColorUtils.parseHexColor(sunriseSunsetColor),
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
        moonPhase = moonPhase,
    )
}
