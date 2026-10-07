package btcrenaud.custombiome.injector

import btcrenaud.custombiome.model.CustomBiomeDefinition
import com.google.gson.JsonObject
import org.bukkit.NamespacedKey

/**
 * Server-side registration of custom biomes.
 *
 * Everything that has to reach into server internals lives behind this interface, so the rest
 * of the extension only ever speaks in [CustomBiomeDefinition]. A platform that cannot support
 * live injection degrades to [UnsupportedBiomeInjector] instead of failing silently.
 */
interface BiomeInjector {

    /** Whether this injector can actually register biomes on the running server. */
    val isSupported: Boolean

    /** Short description of the strategy, written to the server log. */
    val describe: String

    /**
     * Why live injection is unavailable, or null when it works. `/tw biome list` words the strategy
     * from this (an admin-editable snippet) rather than from [describe].
     */
    val unsupportedReason: String? get() = null

    /**
     * Registers [definition] in the live biome registry, inheriting everything it does not
     * override from its base biome.
     */
    fun inject(definition: CustomBiomeDefinition): BiomeInjectionResult

    /**
     * Serialises [definition] to the datapack JSON the server itself would accept, by encoding
     * the base biome and applying the overrides on top. This is what makes a biome survive a
     * restart and exist for world generation.
     */
    fun encodeToJson(definition: CustomBiomeDefinition): JsonObject?

    /**
     * The data pack format of the running server. Hardcoding it is what silently disabled the
     * generated datapack, so it is always read from the server.
     */
    fun dataPackFormat(): Int?

    /**
     * Highest data pack format that still accepts a bare `pack_format` in `pack.mcmeta`; anything
     * newer must declare `min_format` and `max_format`. Null when the server does not say.
     */
    fun legacyDataPackFormatLimit(): Int? = null

    /**
     * Numeric registry id of [key] as the client knows it, needed to build biome packets.
     * Null when the biome is unknown or the platform does not expose the registry.
     */
    fun networkId(key: NamespacedKey): Int?
}

sealed interface BiomeInjectionResult {

    /** The biome is now present in the registry and usable immediately. */
    data object Injected : BiomeInjectionResult

    /** The key was already registered; nothing to do. */
    data object AlreadyPresent : BiomeInjectionResult

    /**
     * Injection did not happen. [reason] is meant to be shown to an operator, not swallowed:
     * the biome will only exist after a restart, via the generated datapack.
     */
    data class Failed(val reason: String, val cause: Throwable? = null) : BiomeInjectionResult
}

/**
 * Fallback used when the running server exposes none of the internals live injection needs.
 * Biomes still reach the world through the generated datapack after a restart.
 */
class UnsupportedBiomeInjector(private val why: String) : BiomeInjector {

    override val isSupported: Boolean = false

    override val describe: String = "unsupported ($why)"

    override val unsupportedReason: String = why

    override fun inject(definition: CustomBiomeDefinition): BiomeInjectionResult =
        BiomeInjectionResult.Failed(why)

    override fun encodeToJson(definition: CustomBiomeDefinition): JsonObject? = null

    override fun dataPackFormat(): Int? = null

    override fun networkId(key: NamespacedKey): Int? = null
}

/** Registry keys this extension owns, used to tell custom biomes from vanilla ones. */
fun NamespacedKey.isVanilla(): Boolean = namespace.equals("minecraft", ignoreCase = true)
