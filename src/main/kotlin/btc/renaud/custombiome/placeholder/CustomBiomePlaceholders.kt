package btc.renaud.custombiome.placeholder

import btc.renaud.custombiome.registry.CustomBiomeRegistry
import btc.renaud.custombiome.util.BiomeResolver
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.extensions.placeholderapi.PlaceholderHandler
import org.bukkit.entity.Player
import org.koin.java.KoinJavaComponent.get

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
    
    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null
        
        val key = params.lowercase()
        val registry = get<CustomBiomeRegistry>(CustomBiomeRegistry::class.java)
        
        return when {
            // Current biome ID: %typewriter_custombiome_current%
            key == "custombiome_current" || key == "custombiome_id" -> {
                player.location.block.biome.key.toString()
            }
            
            // Current biome name: %typewriter_custombiome_name%
            key == "custombiome_name" -> {
                BiomeResolver.readableName(player.location.block.biome)
            }
            
            // Is custom biome: %typewriter_custombiome_is_custom%
            key == "custombiome_is_custom" -> {
                BiomeResolver.isCustomBiome(player.location.block.biome).toString()
            }
            
            // Biome temperature: %typewriter_custombiome_temperature%
            key == "custombiome_temperature" -> {
                val biome = player.location.block.biome
                registry.getDefinition(biome.key)?.temperature?.toString() ?: "unknown"
            }
            
            // Biome downfall: %typewriter_custombiome_downfall%
            key == "custombiome_downfall" -> {
                val biome = player.location.block.biome
                registry.getDefinition(biome.key)?.downfall?.toString() ?: "unknown"
            }
            
            // Custom biome count: %typewriter_custombiome_count%
            key == "custombiome_count" -> {
                registry.count().toString()
            }
            
            // Custom biome list: %typewriter_custombiome_list%
            key == "custombiome_list" -> {
                registry.allDefinitions().joinToString(", ") { it.displayName }
            }
            
            // Biome key only: %typewriter_custombiome_key%
            key == "custombiome_key" -> {
                player.location.block.biome.key.key
            }
            
            // Biome namespace: %typewriter_custombiome_namespace%
            key == "custombiome_namespace" -> {
                player.location.block.biome.key.namespace
            }
            
            // Base biome: %typewriter_custombiome_base%
            key == "custombiome_base" -> {
                val biome = player.location.block.biome
                registry.getDefinition(biome.key)?.baseKey?.toString() ?: biome.key.toString()
            }
            
            else -> null
        }
    }
}
