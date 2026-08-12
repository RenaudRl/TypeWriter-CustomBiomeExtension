package btcrenaud.custombiome.placeholder

import btcrenaud.custombiome.registry.CustomBiomeRegistry
import btcrenaud.custombiome.service.BiomeDiscoveryService
import btcrenaud.custombiome.service.BiomeView
import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.extensions.placeholderapi.PlaceholderHandler
import org.bukkit.entity.Player

/**
 * PlaceholderAPI handler for custom biomes.
 *
 * ```
 * %typewriter_custombiome_current%          biome key under the player
 * %typewriter_custombiome_id%               same as current
 * %typewriter_custombiome_name%             readable display name
 * %typewriter_custombiome_key%              key part only
 * %typewriter_custombiome_namespace%        namespace part only
 * %typewriter_custombiome_is_custom%        "true" / "false"
 * %typewriter_custombiome_temperature%      custom biomes only, "unknown" otherwise
 * %typewriter_custombiome_downfall%         custom biomes only, "unknown" otherwise
 * %typewriter_custombiome_base%             base biome the definition inherits from
 * %typewriter_custombiome_count%            registered custom biomes
 * %typewriter_custombiome_list%             their display names
 * %typewriter_custombiome_discovered_count% biomes this player has visited
 * %typewriter_custombiome_discovered_list%  their keys
 * ```
 *
 * Everything player-facing reads through [BiomeView], so a player under a per-player overlay sees
 * the biome they are actually being shown rather than the one stored in the world.
 */
@Singleton
class CustomBiomePlaceholders : PlaceholderHandler {

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        val key = params.lowercase()

        // Registry-wide values do not depend on a viewer.
        when (key) {
            "count" -> return CustomBiomeRegistry.count().toString()
            "list" -> return CustomBiomeRegistry.allDefinitions().joinToString(", ") { it.displayName }
        }

        if (player == null) return null

        val biome = BiomeView.biomeOf(player)
        val definition = CustomBiomeRegistry.getDefinition(biome.key)

        return when (key) {
            "current", "id" -> biome.key.toString()
            "name" -> BiomeResolver.readableName(biome)
            "key" -> biome.key.key
            "namespace" -> biome.key.namespace
            "is_custom" -> BiomeResolver.isCustomBiome(biome).toString()
            "temperature" -> definition?.temperature?.toString() ?: "unknown"
            "downfall" -> definition?.downfall?.toString() ?: "unknown"
            "base" -> definition?.baseKey?.toString() ?: biome.key.toString()
            "discovered_count" -> BiomeDiscoveryService.count(player).toString()
            "discovered_list" -> BiomeDiscoveryService.all(player).joinToString(", ")
            else -> null
        }
    }
}
