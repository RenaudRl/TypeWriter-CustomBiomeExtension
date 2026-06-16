package btcrenaud.custombiome.placeholder

import btcrenaud.custombiome.registry.CustomBiomeRegistry
import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.extensions.placeholderapi.PlaceholderHandler
import org.bukkit.entity.Player

/**
 * PlaceholderAPI handler for custom biome placeholders.
 *
 * Supported placeholders:
 * - %typewriter_custombiome_current% - Current biome ID
 * - %typewriter_custombiome_name% - Current biome display name
 * - %typewriter_custombiome_is_custom% - "true" or "false"
 * - %typewriter_custombiome_temperature% - Biome temperature (custom only)
 * - %typewriter_custombiome_downfall% - Biome downfall (custom only)
 * - %typewriter_custombiome_count% - Total registered custom biomes
 * - %typewriter_custombiome_list% - Comma-separated list of custom biome names
 */
@Singleton
class CustomBiomePlaceholders : PlaceholderHandler {

    private val registry: CustomBiomeRegistry by lazy {
        org.koin.java.KoinJavaComponent.get(CustomBiomeRegistry::class.java)
    }

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null

        val key = params.lowercase()

        return when {
            key == "custombiome_current" || key == "custombiome_id" -> {
                player.location.block.biome.key.toString()
            }

            key == "custombiome_name" -> {
                BiomeResolver.readableName(player.location.block.biome)
            }

            key == "custombiome_is_custom" -> {
                BiomeResolver.isCustomBiome(player.location.block.biome).toString()
            }

            key == "custombiome_temperature" -> {
                val biome = player.location.block.biome
                registry.getDefinition(biome.key)?.temperature?.toString()
                    ?: "N/A"
            }

            key == "custombiome_downfall" -> {
                val biome = player.location.block.biome
                registry.getDefinition(biome.key)?.downfall?.toString()
                    ?: "N/A"
            }

            key == "custombiome_count" -> {
                registry.count().toString()
            }

            key == "custombiome_list" -> {
                registry.allDefinitions().joinToString(", ") { it.displayName }
            }

            key == "custombiome_key" -> {
                player.location.block.biome.key.key
            }

            key == "custombiome_namespace" -> {
                player.location.block.biome.key.namespace
            }

            key == "custombiome_base" -> {
                val biome = player.location.block.biome
                registry.getDefinition(biome.key)?.baseKey?.toString() ?: biome.key.toString()
            }

            else -> null
        }
    }
}
