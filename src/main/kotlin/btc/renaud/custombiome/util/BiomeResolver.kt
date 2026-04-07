package btc.renaud.custombiome.util

import btc.renaud.custombiome.registry.CustomBiomeRegistry
import com.typewritermc.engine.paper.logger
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves biome identifiers to Biome objects.
 * Supports both vanilla and custom biomes.
 */
object BiomeResolver : KoinComponent {
    
    private val registry: CustomBiomeRegistry by inject()
    private val resolvedCache = ConcurrentHashMap<String, Biome>()
    
    /**
     * Clear the resolution cache.
     */
    fun clearCache() {
        resolvedCache.clear()
    }
    
    /**
     * Resolve multiple biome identifiers to Biome objects.
     * 
     * @param identifiers List of biome identifiers (e.g., "minecraft:plains", "typewriter:my_biome")
     * @param ignoreMissing If true, missing biomes are logged as warnings; otherwise throws exception
     * @return Set of resolved Biome objects
     */
    fun resolveIdentifiers(identifiers: Collection<String>, ignoreMissing: Boolean = false): Set<Biome> {
        if (identifiers.isEmpty()) return emptySet()
        
        val missing = mutableListOf<String>()
        val resolved = mutableSetOf<Biome>()
        
        for (identifier in identifiers) {
            val biome = resolve(identifier)
            if (biome != null) {
                resolved += biome
            } else {
                missing += identifier
            }
        }
        
        handleMissing(missing, ignoreMissing)
        return resolved
    }
    
    /**
     * Resolve biome identifiers to integer codes.
     * 
     * @param values Map of identifier to code
     * @param ignoreMissing If true, missing biomes are logged as warnings; otherwise throws exception
     * @return Map of Biome to code
     */
    fun resolveMapping(values: Map<String, Int>, ignoreMissing: Boolean = false): Map<Biome, Int> {
        if (values.isEmpty()) return emptyMap()
        
        val missing = mutableListOf<String>()
        val resolved = mutableMapOf<Biome, Int>()
        
        for ((identifier, value) in values) {
            val biome = resolve(identifier)
            if (biome != null) {
                resolved[biome] = value
            } else {
                missing += identifier
            }
        }
        
        handleMissing(missing, ignoreMissing)
        return resolved
    }
    
    /**
     * Get a human-readable name for a biome.
     */
    fun readableName(biome: Biome?): String {
        if (biome == null) return "Unknown"
        
        // Check if it's a custom biome
        registry.getDefinition(biome.key)?.let { return it.displayName }
        
        // Format vanilla biome key
        val key = runCatching { biome.key }.getOrNull()
        if (key != null) {
            return formatKey(key)
        }
        
        return biome.toString()
    }
    
    /**
     * Check if a biome is a custom biome from this extension.
     */
    fun isCustomBiome(biome: Biome): Boolean {
        return registry.getDefinition(biome.key) != null
    }
    
    /**
     * Resolve a single biome identifier.
     * 
     * @param identifier Biome identifier (e.g., "minecraft:plains", "plains", "typewriter:my_biome")
     * @return Resolved Biome or null if not found
     */
    fun resolve(identifier: String): Biome? {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty()) return null
        
        // Check cache
        resolvedCache[trimmed]?.let { return it }
        
        // Try vanilla first
        resolveVanilla(trimmed)?.let { 
            resolvedCache[trimmed] = it
            return it 
        }
        
        // Try custom biomes
        resolveCustom(trimmed)?.let {
            resolvedCache[trimmed] = it
            return it
        }
        
        return null
    }

    /**
     * Resolve a biome identifier to a NamespacedKey.
     */
    fun resolveKey(identifier: String): NamespacedKey? {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty()) return null

        if (trimmed.contains(':')) {
            return NamespacedKey.fromString(trimmed.lowercase(Locale.ENGLISH))
        }

        return NamespacedKey.minecraft(trimmed.lowercase(Locale.ENGLISH))
    }
    
    private fun resolveVanilla(identifier: String): Biome? {
        // If it defines a namespace that is NOT minecraft, it's not a vanilla biome
        if (identifier.contains(':') && !identifier.startsWith("minecraft:")) {
            return null
        }

        val normalized = identifier
            .removePrefix("minecraft:")
            .replace('-', '_')
            .replace(' ', '_')
            .lowercase(Locale.ENGLISH)
        
        // Validate characters before creating key to verify strict compliance
        if (!normalized.all { it.isLetterOrDigit() || it == '_' || it == '.' || it == '/' }) {
             return null
        }

        val key = NamespacedKey.minecraft(normalized)
        return RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.BIOME)
            .get(key)
    }
    
    private fun resolveCustom(identifier: String): Biome? {
        // Parse key
        val key = NamespacedKey.fromString(identifier.lowercase(Locale.ENGLISH))
            ?: return null
        
        // Check if it's in our registry
        if (registry.getDefinition(key) == null) return null
        
        // Resolve from Bukkit registry
        return RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.BIOME)
            .get(key)
    }
    
    private fun handleMissing(missing: List<String>, ignoreMissing: Boolean) {
        if (missing.isEmpty()) return
        
        val message = "Unable to resolve biomes: ${missing.joinToString()}"
        if (ignoreMissing) {
            logger.warning("$message (ignoring)")
        } else {
            throw IllegalArgumentException(message)
        }
    }
    
    private fun formatKey(key: NamespacedKey): String {
        val formatted = key.key.split('_', '-', '/').filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase(Locale.ENGLISH).replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.ENGLISH) else char.toString()
                }
            }
        
        return if (key.namespace.equals("minecraft", ignoreCase = true)) {
            formatted
        } else {
            "${key.namespace}:$formatted"
        }
    }
}
