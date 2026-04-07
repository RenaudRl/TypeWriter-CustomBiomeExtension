package btc.renaud.custombiome.entries.variable

import btc.renaud.custombiome.registry.CustomBiomeRegistry
import btc.renaud.custombiome.util.BiomeResolver
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.entries.VariableEntry
import com.typewritermc.engine.paper.entry.entries.VarContext
import com.typewritermc.engine.paper.entry.entries.cast
import org.koin.java.KoinJavaComponent.get

/**
 * Variable that returns the player's current biome name.
 */
@Tags("variable")
@Entry(
    "current_biome_variable",
    "Returns the player's current biome name",
    Colors.GREEN,
    icon = "mdi:pine-tree"
)
class CurrentBiomeVariableEntry(
    override val id: String = "",
    override val name: String = "",
    
    @Help("Format for the output: ID returns 'namespace:key', NAME returns readable name")
    val format: BiomeFormat = BiomeFormat.NAME,
    
) : VariableEntry {
    
    override fun <T : Any> get(context: VarContext<T>): T {
        val player = context.player
        val biome = player.location.block.biome
        
        val result = when (format) {
            BiomeFormat.ID -> biome.key.toString()
            BiomeFormat.NAME -> BiomeResolver.readableName(biome)
            BiomeFormat.KEY -> biome.key.key
            BiomeFormat.NAMESPACE -> biome.key.namespace
        }
        
        return context.cast(result)
    }
    
    enum class BiomeFormat {
        @Help("Returns the full key (e.g., 'minecraft:plains')")
        ID,
        
        @Help("Returns a human-readable name (e.g., 'Plains')")
        NAME,
        
        @Help("Returns just the key part (e.g., 'plains')")
        KEY,
        
        @Help("Returns just the namespace (e.g., 'minecraft' or 'typewriter')")
        NAMESPACE,
    }
}

/**
 * Variable that returns information about a specific biome property.
 */
@Tags("variable")
@Entry(
    "biome_property_variable",
    "Returns a property of the player's current biome",
    Colors.GREEN,
    icon = "mdi:thermometer"
)
class BiomePropertyVariableEntry(
    override val id: String = "",
    override val name: String = "",
    
    @Help("Which property to return")
    val property: BiomeProperty = BiomeProperty.IS_CUSTOM,
    
) : VariableEntry {
    
    override fun <T : Any> get(context: VarContext<T>): T {
        val player = context.player
        val biome = player.location.block.biome
        val registry = get<CustomBiomeRegistry>(CustomBiomeRegistry::class.java)
        val definition = registry.getDefinition(biome.key)
        
        val result = when (property) {
            BiomeProperty.IS_CUSTOM -> BiomeResolver.isCustomBiome(biome).toString()
            BiomeProperty.TEMPERATURE -> definition?.temperature?.toString() ?: "unknown"
            BiomeProperty.DOWNFALL -> definition?.downfall?.toString() ?: "unknown"
            BiomeProperty.BASE_BIOME -> definition?.baseKey?.toString() ?: biome.key.toString()
        }
        
        return context.cast(result)
    }
    
    enum class BiomeProperty {
        @Help("Returns 'true' if in a custom biome, 'false' otherwise")
        IS_CUSTOM,
        
        @Help("Returns the temperature value (only for custom biomes)")
        TEMPERATURE,
        
        @Help("Returns the downfall/humidity value (only for custom biomes)")
        DOWNFALL,
        
        @Help("Returns the base biome key")
        BASE_BIOME,
    }
}

/**
 * Variable that returns the list of custom biomes.
 */
@Tags("variable")
@Entry(
    "custom_biome_list_variable",
    "Returns a list of registered custom biomes",
    Colors.GREEN,
    icon = "mdi:format-list-bulleted"
)
class CustomBiomeListVariableEntry(
    override val id: String = "",
    override val name: String = "",
    
    @Help("Separator between biome names (default: ', ')")
    val separator: String = ", ",
    
    @Help("Format for each biome: ID or NAME")
    val format: CurrentBiomeVariableEntry.BiomeFormat = CurrentBiomeVariableEntry.BiomeFormat.NAME,
    
) : VariableEntry {
    
    override fun <T : Any> get(context: VarContext<T>): T {
        val registry = get<CustomBiomeRegistry>(CustomBiomeRegistry::class.java)
        val definitions = registry.allDefinitions()
        
        val result = definitions.joinToString(separator) { def ->
            when (format) {
                CurrentBiomeVariableEntry.BiomeFormat.ID -> def.key.toString()
                CurrentBiomeVariableEntry.BiomeFormat.NAME -> def.displayName
                CurrentBiomeVariableEntry.BiomeFormat.KEY -> def.key.key
                CurrentBiomeVariableEntry.BiomeFormat.NAMESPACE -> def.key.namespace
            }
        }
        
        return context.cast(result)
    }
}
