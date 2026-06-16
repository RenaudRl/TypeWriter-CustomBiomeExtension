package btcrenaud.custombiome.model

import org.bukkit.NamespacedKey

/**
 * Runtime representation of a custom biome definition.
 */
data class CustomBiomeDefinition(
    /** Unique key for this biome */
    val key: NamespacedKey,
    /** Base biome to inherit from (null = plains) */
    val baseKey: NamespacedKey?,
    /** Temperature value (0.0 = cold, 2.0 = hot) */
    val temperature: Double?,
    /** Downfall/humidity value (0.0 = dry, 1.0 = wet) */
    val downfall: Double?,
    /** Color configuration */
    val colors: BiomeColors,
    val attributes: BiomeAttributes = BiomeAttributes()
) {
    /** Display name derived from the key */
    val displayName: String
        get() = key.key.replace('_', ' ').split(' ')
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}
