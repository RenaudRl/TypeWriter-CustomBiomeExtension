package btcrenaud.custombiome.entries.audience

import btcrenaud.custombiome.entries.manifest.CustomBiomeDefinitionEntry
import btcrenaud.custombiome.service.BiomeSource
import btcrenaud.custombiome.service.BiomeView
import btcrenaud.custombiome.util.BiomeResolver
import btcrenaud.custombiome.util.BiomeSelection
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
    
    @Help("Custom biomes to filter on, picked from their definitions")
    val customBiomes: List<Ref<CustomBiomeDefinitionEntry>> = emptyList(),

    @Help("Vanilla biome identifiers to filter on. Examples: 'minecraft:plains'")
    val biomes: List<String> = emptyList(),
    
    @Help("If true, ignore missing biome identifiers instead of throwing errors")
    val ignoreMissing: Boolean = true,
    
    @Help("If true, only include players in custom biomes (ignores biome list)")
    val customBiomesOnly: Boolean = false,
    
    @Help("Whether to filter on what the player is shown (overlays included) or what the world contains")
    val source: BiomeSource = BiomeSource.PLAYER_VIEW,

    override val inverted: Boolean = false,

) : AudienceFilterEntry, Invertible {

    override suspend fun display(): AudienceFilter = BiomeAudienceFilter(
        ref(),
        customBiomes,
        biomes,
        ignoreMissing,
        customBiomesOnly,
        source
    )

    override fun parser() = placeholderParser {
        include(super.parser())
        literal("biome_name") {
            supplyPlayer { player -> BiomeResolver.readableName(BiomeView.biomeOf(player, source)) }
        }
        literal("biome_id") {
            supplyPlayer { player -> BiomeView.biomeOf(player, source).key.toString() }
        }
        literal("is_custom") {
            supplyPlayer { player -> BiomeResolver.isCustomBiome(BiomeView.biomeOf(player, source)).toString() }
        }
    }
}

class BiomeAudienceFilter(
    ref: Ref<out AudienceFilterEntry>,
    private val customBiomes: List<Ref<CustomBiomeDefinitionEntry>>,
    private val biomes: List<String>,
    private val ignoreMissing: Boolean,
    private val customBiomesOnly: Boolean,
    private val source: BiomeSource,
) : AudienceFilter(ref) {

    override fun filter(player: Player): Boolean {
        val currentBiome = BiomeView.biomeOf(player, source)

        if (customBiomesOnly) {
            return BiomeResolver.isCustomBiome(currentBiome)
        }

        // No selection at all means "any biome".
        if (BiomeSelection.isEmpty(customBiomes, biomes)) return true

        val targetBiomes = BiomeSelection.resolve(customBiomes, biomes, ignoreMissing)
        if (targetBiomes.isEmpty()) return false

        return currentBiome in targetBiomes
    }
}
