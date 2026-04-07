package btc.renaud.custombiome.model

/**
 * Represents the "attributes" section of a biome definition in Minecraft 1.21.11+.
 * These correspond to keys in the "minecraft:environment_attribute" registry,
 * typically prefixed with "visual/".
 */
data class BiomeAttributes(
    // Colors
    val sky: Int? = null,
    val fog: Int? = null,
    val waterFog: Int? = null,
    val cloud: Int? = null,
    val skyLight: Int? = null,
    val sunriseSunset: Int? = null,

    // Fog Distances
    val fogStartDistance: Float? = null,
    val fogEndDistance: Float? = null,
    val skyFogEndDistance: Float? = null,
    val waterFogStartDistance: Float? = null,
    val waterFogEndDistance: Float? = null,
    val cloudFogEndDistance: Float? = null,

    // Cloud & Light
    val cloudHeight: Float? = null,
    val skyLightFactor: Float? = null,

    // Celestial
    val sunAngle: Float? = null,
    val moonAngle: Float? = null,
    val starAngle: Float? = null,
    val starBrightness: Float? = null,
    val moonPhase: String? = null, // e.g. "full_moon"

    // Particles (Simplified/Pending full support)
    // val defaultDripstoneParticle: String? = null
    // val ambientParticles: List<...>? = null
)
