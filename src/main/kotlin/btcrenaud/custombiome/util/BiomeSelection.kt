package btcrenaud.custombiome.util

import btcrenaud.custombiome.entries.manifest.CustomBiomeDefinitionEntry
import com.typewritermc.core.entries.Ref
import org.bukkit.block.Biome

/**
 * Resolves the two ways an entry can point at biomes.
 *
 * Free-text identifiers stay available for vanilla biomes, where typing `minecraft:plains` is the
 * natural thing to do. Custom biomes are referenced through their defining entry instead, so the
 * editor offers completion and renaming a biome does not silently break every page that used it.
 */
object BiomeSelection {

    fun resolve(
        references: List<Ref<CustomBiomeDefinitionEntry>>,
        identifiers: List<String>,
        ignoreMissing: Boolean,
    ): Set<Biome> {
        val fromRefs = references.mapNotNull { ref ->
            ref.get()?.key?.let { BiomeResolver.resolve(it.toString()) }
        }
        return fromRefs.toSet() + BiomeResolver.resolveIdentifiers(identifiers, ignoreMissing)
    }

    /** True when the selection is empty, meaning "any biome". */
    fun isEmpty(
        references: List<Ref<CustomBiomeDefinitionEntry>>,
        identifiers: List<String>,
    ): Boolean = references.isEmpty() && identifiers.isEmpty()
}
