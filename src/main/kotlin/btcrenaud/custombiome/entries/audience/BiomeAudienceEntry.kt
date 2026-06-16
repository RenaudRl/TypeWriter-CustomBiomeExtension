package btcrenaud.custombiome.entries.audience

import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.AudienceFilter
import com.typewritermc.engine.paper.entry.entries.AudienceFilterEntry
import com.typewritermc.engine.paper.entry.entries.Invertible
import com.typewritermc.engine.paper.entry.include
import com.typewritermc.engine.paper.entry.literal
import com.typewritermc.engine.paper.entry.placeholderParser
import com.typewritermc.engine.paper.entry.supplyPlayer
import org.bukkit.entity.Player

/**
 * Audience filter that includes players based on their current biome.
 * Supports both vanilla and custom biomes.
 */
@Entry(
    "biome_audience",
    "Filter players based on the biome they are standing in",
    Colors.GREEN,
    icon = "mdi:pine-tree"
)
class BiomeAudienceEntry(
    override val id: String = "",
    override val name: String = "",
    override val children: List<Ref<out AudienceEntry>> = emptyList(),
    
    @Help("Biome identifiers to filter on. Examples: 'minecraft:plains', 'typewriter:my_biome'")
    val biomes: List<String> = emptyList(),
    
    @Help("If true, ignore missing biome identifiers instead of throwing errors")
    val ignoreMissing: Boolean = true,
    
    @Help("If true, only include players in custom biomes (ignores biome list)")
    val customBiomesOnly: Boolean = false,
    
    override val inverted: Boolean = false,
    
) : AudienceFilterEntry, Invertible {
    
    override suspend fun display(): AudienceFilter = BiomeAudienceFilter(
        ref(),
        biomes,
        ignoreMissing,
        customBiomesOnly
    )
    
    override fun parser() = placeholderParser {
        include(super.parser())
        literal("biome_name") {
            supplyPlayer { player -> BiomeResolver.readableName(player.location.block.biome) }
        }
        literal("biome_id") {
            supplyPlayer { player -> player.location.block.biome.key.toString() }
        }
        literal("is_custom") {
            supplyPlayer { player -> BiomeResolver.isCustomBiome(player.location.block.biome).toString() }
        }
    }
}

class BiomeAudienceFilter(
    ref: Ref<out AudienceFilterEntry>,
    private val biomes: List<String>,
    private val ignoreMissing: Boolean,
    private val customBiomesOnly: Boolean,
) : AudienceFilter(ref) {
    
    override fun filter(player: Player): Boolean {
        val currentBiome = player.location.block.biome
        
        if (customBiomesOnly) {
            return BiomeResolver.isCustomBiome(currentBiome)
        }
        
        if (biomes.isEmpty()) {
            // No filter specified - include all players
            return true
        }
        
        val targetBiomes = BiomeResolver.resolveIdentifiers(biomes, ignoreMissing)
        if (targetBiomes.isEmpty()) return false
        
        return currentBiome in targetBiomes
    }
}
