package btcrenaud.custombiome.entries.event

import btcrenaud.custombiome.entries.manifest.CustomBiomeDefinitionEntry
import btcrenaud.custombiome.service.BiomeTrackingService
import btcrenaud.custombiome.util.BiomeResolver
import btcrenaud.custombiome.util.BiomeSelection
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.ContextKeys
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.EntryListener
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.KeyType
import com.typewritermc.core.entries.Query
import com.typewritermc.core.interaction.EntryContextKey
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.EventEntry
import com.typewritermc.engine.paper.entry.triggerAllFor
import org.bukkit.Bukkit
import org.bukkit.block.Biome
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Context keys for biome events.
 */
enum class BiomeEventContextKeys(override val klass: KClass<*>) : EntryContextKey {
    @KeyType(String::class)
    BIOME_ID(String::class),
    
    @KeyType(String::class)
    BIOME_NAME(String::class),
    
    @KeyType(String::class)
    PREVIOUS_BIOME_ID(String::class),
    
    @KeyType(String::class)
    PREVIOUS_BIOME_NAME(String::class),
    
    @KeyType(Boolean::class)
    IS_CUSTOM_BIOME(Boolean::class),
}

/**
 * Event triggered when a player enters a specific biome.
 */
@Entry(
    "enter_biome_event",
    "Triggered when a player enters a biome",
    Colors.YELLOW,
    icon = "mdi:pine-tree"
)
@ContextKeys(BiomeEventContextKeys::class)
class EnterBiomeEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    
    @Help("Biome identifiers to trigger on (empty = any biome). Examples: 'minecraft:plains', 'typewriter:my_biome'")
    val biomes: List<String> = emptyList(),
    
    @Help("Custom biomes to trigger on, picked from their definitions")
    val customBiomes: List<Ref<CustomBiomeDefinitionEntry>> = emptyList(),

    @Help("If true, ignore missing biome identifiers instead of throwing errors")
    val ignoreMissing: Boolean = true,

) : EventEntry

/**
 * Event triggered when a player leaves a specific biome.
 */
@Entry(
    "leave_biome_event",
    "Triggered when a player leaves a biome",
    Colors.YELLOW,
    icon = "mdi:pine-tree-off"
)
@ContextKeys(BiomeEventContextKeys::class)
class LeaveBiomeEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    
    @Help("Biome identifiers to trigger when leaving (empty = any biome). Examples: 'minecraft:plains', 'typewriter:my_biome'")
    val biomes: List<String> = emptyList(),
    
    @Help("Custom biomes to trigger on, picked from their definitions")
    val customBiomes: List<Ref<CustomBiomeDefinitionEntry>> = emptyList(),

    @Help("If true, ignore missing biome identifiers instead of throwing errors")
    val ignoreMissing: Boolean = true,

) : EventEntry

/**
 * Biome transitions are only worth recomputing when the player crosses a 4×4 cell boundary:
 * biomes are stored per quart, so every other move cannot possibly change the answer. This keeps
 * the hottest event on the server cheap.
 */
private fun PlayerMoveEvent.crossedQuart(): Boolean =
    (from.blockX shr 2) != (to.blockX shr 2) ||
        (from.blockY shr 2) != (to.blockY shr 2) ||
        (from.blockZ shr 2) != (to.blockZ shr 2)

@EntryListener(EnterBiomeEventEntry::class)
fun onPlayerMoveEnterBiome(event: PlayerMoveEvent, query: Query<EnterBiomeEventEntry>) {
    if (!event.crossedQuart()) return

    val transition = BiomeTrackingService.observe(event.player, event.to.block.biome) ?: return
    val entered = transition.to

    query.findWhere { entry ->
        if (BiomeSelection.isEmpty(entry.customBiomes, entry.biomes)) return@findWhere true
        entered in BiomeSelection.resolve(entry.customBiomes, entry.biomes, entry.ignoreMissing)
    }.triggerAllFor(event.player) {
        BiomeEventContextKeys.BIOME_ID withValue entered.key.toString()
        BiomeEventContextKeys.BIOME_NAME withValue BiomeResolver.readableName(entered)
        BiomeEventContextKeys.PREVIOUS_BIOME_ID withValue (transition.from?.key?.toString() ?: "")
        BiomeEventContextKeys.PREVIOUS_BIOME_NAME withValue
            (transition.from?.let { BiomeResolver.readableName(it) } ?: "")
        BiomeEventContextKeys.IS_CUSTOM_BIOME withValue BiomeResolver.isCustomBiome(entered)
    }
}

@EntryListener(LeaveBiomeEventEntry::class)
fun onPlayerMoveLeaveBiome(event: PlayerMoveEvent, query: Query<LeaveBiomeEventEntry>) {
    if (!event.crossedQuart()) return

    val transition = BiomeTrackingService.observe(event.player, event.to.block.biome) ?: return
    // Nothing was left when the player had no tracked biome yet (first move after joining).
    val left = transition.from ?: return

    query.findWhere { entry ->
        if (BiomeSelection.isEmpty(entry.customBiomes, entry.biomes)) return@findWhere true
        left in BiomeSelection.resolve(entry.customBiomes, entry.biomes, entry.ignoreMissing)
    }.triggerAllFor(event.player) {
        BiomeEventContextKeys.BIOME_ID withValue left.key.toString()
        BiomeEventContextKeys.BIOME_NAME withValue BiomeResolver.readableName(left)
        BiomeEventContextKeys.PREVIOUS_BIOME_ID withValue transition.to.key.toString()
        BiomeEventContextKeys.PREVIOUS_BIOME_NAME withValue BiomeResolver.readableName(transition.to)
        BiomeEventContextKeys.IS_CUSTOM_BIOME withValue BiomeResolver.isCustomBiome(left)
    }
}
