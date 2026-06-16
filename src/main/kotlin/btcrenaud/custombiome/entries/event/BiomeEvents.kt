package btcrenaud.custombiome.entries.event

import btcrenaud.custombiome.util.BiomeResolver
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

    @Help("If true, ignore missing biome identifiers instead of throwing errors")
    val ignoreMissing: Boolean = true,

) : EventEntry {

    companion object {
        private val lastBiomes = ConcurrentHashMap<UUID, Biome>()

        fun getLastBiome(playerId: UUID): Biome? = lastBiomes[playerId]
        fun setLastBiome(playerId: UUID, biome: Biome) { lastBiomes[playerId] = biome }
        fun removePlayer(playerId: UUID) { lastBiomes.remove(playerId) }
    }
}

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

    @Help("If true, ignore missing biome identifiers instead of throwing errors")
    val ignoreMissing: Boolean = true,

) : EventEntry {

    companion object {
        private val lastBiomes = ConcurrentHashMap<UUID, Biome>()

        fun getLastBiome(playerId: UUID): Biome? = lastBiomes[playerId]
        fun setLastBiome(playerId: UUID, biome: Biome) { lastBiomes[playerId] = biome }
        fun removePlayer(playerId: UUID) { lastBiomes.remove(playerId) }
    }
}

@EntryListener(EnterBiomeEventEntry::class)
fun onPlayerMoveEnterBiome(event: PlayerMoveEvent, query: Query<EnterBiomeEventEntry>) {
    val player = event.player
    val from = event.from
    val to = event.to

    if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) return

    val currentBiome = to.block.biome
    val previousBiome = EnterBiomeEventEntry.getLastBiome(player.uniqueId)

    EnterBiomeEventEntry.setLastBiome(player.uniqueId, currentBiome)

    if (previousBiome == currentBiome) return

    val currentBiomeId = currentBiome.key.toString()
    val currentBiomeName = BiomeResolver.readableName(currentBiome)
    val previousBiomeId = previousBiome?.key?.toString() ?: ""
    val previousBiomeName = previousBiome?.let { BiomeResolver.readableName(it) } ?: ""
    val isCustom = BiomeResolver.isCustomBiome(currentBiome)

    query.findWhere { entry ->
        if (entry.biomes.isEmpty()) return@findWhere true
        val targetBiomes = BiomeResolver.resolveIdentifiers(entry.biomes, entry.ignoreMissing)
        currentBiome in targetBiomes
    }.triggerAllFor(player) {
        BiomeEventContextKeys.BIOME_ID withValue currentBiomeId
        BiomeEventContextKeys.BIOME_NAME withValue currentBiomeName
        BiomeEventContextKeys.PREVIOUS_BIOME_ID withValue previousBiomeId
        BiomeEventContextKeys.PREVIOUS_BIOME_NAME withValue previousBiomeName
        BiomeEventContextKeys.IS_CUSTOM_BIOME withValue isCustom
    }
}

@EntryListener(LeaveBiomeEventEntry::class)
fun onPlayerMoveLeaveBiome(event: PlayerMoveEvent, query: Query<LeaveBiomeEventEntry>) {
    val player = event.player
    val from = event.from
    val to = event.to

    if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) return

    val currentBiome = to.block.biome
    val previousBiome = LeaveBiomeEventEntry.getLastBiome(player.uniqueId)

    LeaveBiomeEventEntry.setLastBiome(player.uniqueId, currentBiome)

    if (previousBiome == null || previousBiome == currentBiome) return

    val previousBiomeId = previousBiome.key.toString()
    val previousBiomeName = BiomeResolver.readableName(previousBiome)
    val currentBiomeId = currentBiome.key.toString()
    val currentBiomeName = BiomeResolver.readableName(currentBiome)
    val isCustom = BiomeResolver.isCustomBiome(previousBiome)

    query.findWhere { entry ->
        if (entry.biomes.isEmpty()) return@findWhere true
        val targetBiomes = BiomeResolver.resolveIdentifiers(entry.biomes, entry.ignoreMissing)
        previousBiome in targetBiomes
    }.triggerAllFor(player) {
        BiomeEventContextKeys.BIOME_ID withValue previousBiomeId
        BiomeEventContextKeys.BIOME_NAME withValue previousBiomeName
        BiomeEventContextKeys.PREVIOUS_BIOME_ID withValue currentBiomeId
        BiomeEventContextKeys.PREVIOUS_BIOME_NAME withValue currentBiomeName
        BiomeEventContextKeys.IS_CUSTOM_BIOME withValue isCustom
    }
}
