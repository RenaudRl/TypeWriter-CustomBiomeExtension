package btcrenaud.custombiome.util

import org.bukkit.NamespacedKey
import java.util.Locale

/**
 * Turns what a content creator types into a registry key.
 *
 * Registry keys only accept lowercase letters, digits and a few separators, while the entry fields
 * are free text. Normalising in one place is what keeps registration, lookup and unregistration
 * agreeing on the same key — they used to each re-derive it.
 */
object BiomeKeys {

    const val DEFAULT_NAMESPACE = "typewriter"

    fun of(namespace: String, id: String): NamespacedKey =
        NamespacedKey(normalizeNamespace(namespace), normalizeId(id))

    fun normalizeId(raw: String): String = raw
        .trim()
        .lowercase(Locale.ENGLISH)
        .replace(' ', '_')
        .replace('-', '_')
        .filter { it.isLetterOrDigit() || it == '_' || it == '.' }
        .ifEmpty { "custom_biome" }

    fun normalizeNamespace(raw: String): String = raw
        .trim()
        .lowercase(Locale.ENGLISH)
        .replace(' ', '_')
        .replace('-', '_')
        .filter { it.isLetterOrDigit() || it == '_' || it == '.' }
        .ifEmpty { DEFAULT_NAMESPACE }
}
