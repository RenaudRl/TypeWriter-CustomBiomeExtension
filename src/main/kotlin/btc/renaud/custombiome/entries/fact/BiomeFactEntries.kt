package btc.renaud.custombiome.entries.fact

import btc.renaud.custombiome.registry.CustomBiomeRegistry
import btc.renaud.custombiome.util.BiomeResolver
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.MultiLine
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.entry.include
import com.typewritermc.engine.paper.entry.literal
import com.typewritermc.engine.paper.entry.placeholderParser
import com.typewritermc.engine.paper.entry.supplyPlayer
import com.typewritermc.engine.paper.facts.FactData
import org.bukkit.entity.Player
import org.koin.java.KoinJavaComponent.get

/**
 * Fact entry that exposes player's current biome as a fact value.
 * Supports both vanilla and custom biomes.
 */
@Entry(
    "player_biome_fact",
    "Expose the player's current biome as a fact value",
    Colors.PURPLE,
    icon = "mdi:pine-tree"
)
class PlayerBiomeFactEntry(
    override val id: String = "",
    override val name: String = "",
    
    @MultiLine
    @Help("A comment to keep track of what this fact is used for.")
    override val comment: String = "",
    
    @Help("If left empty, every player has its own group.")
    override val group: Ref<GroupEntry> = emptyRef(),
    
    @Help("Mode for determining the fact value")
    val mode: Mode = Mode.BOOLEAN,
    
    @Help("Biome identifiers for BOOLEAN mode. Returns 1 if player is in any of these biomes, 0 otherwise.")
    val biomes: List<String> = emptyList(),
    
    @Help("Biome to code mapping for CODE mode. Returns the code for the current biome.")
    val codes: Map<String, Int> = emptyMap(),
    
    @Help("Default code to return if current biome is not in the mapping")
    val defaultCode: Int = 0,
    
    @Help("If true, ignore missing biome identifiers instead of throwing errors")
    val ignoreMissing: Boolean = true,
    
) : ReadableFactEntry {
    
    override fun readSinglePlayer(player: Player): FactData {
        val biome = player.location.block.biome
        val value = when (mode) {
            Mode.BOOLEAN -> {
                if (biomes.isEmpty()) {
                    // No biomes specified - return 1 if in any custom biome
                    if (BiomeResolver.isCustomBiome(biome)) 1 else 0
                } else {
                    val targetBiomes = BiomeResolver.resolveIdentifiers(biomes, ignoreMissing)
                    if (targetBiomes.isEmpty()) 0 else if (biome in targetBiomes) 1 else 0
                }
            }
            Mode.CODE -> {
                val codedBiomes = BiomeResolver.resolveMapping(codes, ignoreMissing)
                codedBiomes[biome] ?: defaultCode
            }
            Mode.HASH -> {
                biome.key.toString().hashCode()
            }
        }
        return FactData(value)
    }
    
    override fun parser() = placeholderParser {
        include(super.parser())
        literal("biome_id") {
            supplyPlayer { player -> player.location.block.biome.key.toString() }
        }
        literal("biome_name") {
            supplyPlayer { player -> BiomeResolver.readableName(player.location.block.biome) }
        }
        literal("is_custom") {
            supplyPlayer { player -> BiomeResolver.isCustomBiome(player.location.block.biome).toString() }
        }
    }
    
    enum class Mode {
        @Help("Returns 1 if player is in any of the specified biomes, 0 otherwise")
        BOOLEAN,
        
        @Help("Returns the code mapped to the current biome")
        CODE,
        
        @Help("Returns the hash code of the biome key")
        HASH,
    }
}

/**
 * Fact entry that returns 1 if the player is in any custom biome, 0 otherwise.
 */
@Entry(
    "is_in_custom_biome_fact",
    "Returns 1 if player is in a custom biome, 0 otherwise",
    Colors.PURPLE,
    icon = "mdi:pine-tree-box"
)
class IsInCustomBiomeFactEntry(
    override val id: String = "",
    override val name: String = "",
    
    @MultiLine
    @Help("A comment to keep track of what this fact is used for.")
    override val comment: String = "",
    
    @Help("If left empty, every player has its own group.")
    override val group: Ref<GroupEntry> = emptyRef(),
    
) : ReadableFactEntry {
    
    override fun readSinglePlayer(player: Player): FactData {
        val biome = player.location.block.biome
        val isCustom = BiomeResolver.isCustomBiome(biome)
        return FactData(if (isCustom) 1 else 0)
    }
    
    override fun parser() = placeholderParser {
        include(super.parser())
        literal("biome_name") {
            supplyPlayer { player -> BiomeResolver.readableName(player.location.block.biome) }
        }
    }
}

/**
 * Fact entry that returns the count of registered custom biomes.
 */
@Entry(
    "custom_biome_count_fact",
    "Returns the number of registered custom biomes",
    Colors.PURPLE,
    icon = "mdi:counter"
)
class CustomBiomeCountFactEntry(
    override val id: String = "",
    override val name: String = "",
    
    @MultiLine
    @Help("A comment to keep track of what this fact is used for.")
    override val comment: String = "",
    
    @Help("If left empty, every player has its own group.")
    override val group: Ref<GroupEntry> = emptyRef(),
    
) : ReadableFactEntry {
    
    override fun readSinglePlayer(player: Player): FactData {
        val registry = get<CustomBiomeRegistry>(CustomBiomeRegistry::class.java)
        return FactData(registry.count())
    }
}
