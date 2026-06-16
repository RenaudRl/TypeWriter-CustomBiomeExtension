package btcrenaud.custombiome.model

/**
 * Color configuration for a custom biome.
 * All colors are optional - if null, the base biome's color is used.
 */
data class BiomeColors(
    /** Fog color (RGB integer) */
    val fog: Int? = null,
    /** Water color (RGB integer) */
    val water: Int? = null,
    /** Underwater fog color (RGB integer) */
    val waterFog: Int? = null,
    /** Sky color (RGB integer) */
    val sky: Int? = null,
    /** Foliage/leaf color (RGB integer) */
    val foliage: Int? = null,
    /** Grass color (RGB integer) */
    val grass: Int? = null,
    /** Dry foliage color for badlands/savanna biomes (RGB integer) - MC 1.21.5+ */
    val dryFoliage: Int? = null,
    /** Sunrise/sunset sky tint color (RGB integer) - MC 1.21.11+ */
    val sunriseSunset: Int? = null,
    /** Cloud color (RGB integer) - MC 1.21.11+ */
    val cloud: Int? = null,
    /** Skylight color tint (RGB integer) - MC 1.21.11+ */
    val skyLight: Int? = null
) {
    companion object {
        val EMPTY = BiomeColors()
    }
}
