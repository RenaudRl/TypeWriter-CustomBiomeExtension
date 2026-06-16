package btcrenaud.custombiome.util

import btcrenaud.custombiome.registry.CustomBiomeRegistry
import com.typewritermc.engine.paper.logger
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves biome identifiers to Biome objects.
 * Supports both vanilla and custom biomes.
 */
object BiomeResolver {

    private val resolvedCache = ConcurrentHashMap<String, Biome>()

    private val registry: CustomBiomeRegistry by lazy {
        org.koin.java.KoinJavaComponent.get(CustomBiomeRegistry::class.java)
    }

    fun clearCache() {
        resolvedCache.clear()
    }

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

    fun readableName(biome: Biome?): String {
        if (biome == null) return "Unknown"

        registry.getDefinition(biome.key)?.let { return it.displayName }

        val key = runCatching { biome.key }.getOrNull()
        if (key != null) {
            return formatKey(key)
        }

        return biome.toString()
    }

    fun isCustomBiome(biome: Biome): Boolean {
        return registry.getDefinition(biome.key) != null
    }

    fun resolve(identifier: String): Biome? {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty()) return null

        resolvedCache[trimmed]?.let { return it }

        resolveVanilla(trimmed)?.let {
            resolvedCache[trimmed] = it
            return it
        }

        resolveCustom(trimmed)?.let {
            resolvedCache[trimmed] = it
            return it
        }

        return null
    }

    fun resolveKey(identifier: String): NamespacedKey? {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty()) return null

        if (trimmed.contains(':')) {
            return NamespacedKey.fromString(trimmed.lowercase(Locale.ENGLISH))
        }

        return NamespacedKey.minecraft(trimmed.lowercase(Locale.ENGLISH))
    }

    private fun resolveVanilla(identifier: String): Biome? {
        if (identifier.contains(':') && !identifier.startsWith("minecraft:")) {
            return null
        }

        val normalized = identifier
            .removePrefix("minecraft:")
            .replace('-', '_')
            .replace(' ', '_')
            .lowercase(Locale.ENGLISH)

        if (!normalized.all { it.isLetterOrDigit() || it == '_' || it == '.' || it == '/' }) {
            return null
        }

        val key = NamespacedKey.minecraft(normalized)
        return RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.BIOME)
            .get(key)
    }

    private fun resolveCustom(identifier: String): Biome? {
        val key = NamespacedKey.fromString(identifier.lowercase(Locale.ENGLISH))
            ?: return null

        if (registry.getDefinition(key) == null) return null

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
