package btcrenaud.custombiome.entries.objective

import btcrenaud.custombiome.service.BiomeDiscoveryService
import btcrenaud.custombiome.service.BiomeExploration
import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Colored
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.quest.entries.ObjectiveEntry
import com.typewritermc.quest.entries.QuestEntry
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import java.util.Optional

/**
 * A quest objective completed by visiting biomes.
 *
 * It reads the same discovery record the tracker fills in, so "go find the corrupted forest" needs
 * no trigger wiring: walking into the biome is the completion condition. The check runs when the
 * player crosses into a biome, never on every step (see [ExploreBiomeListener]), and fires
 * [triggers] while the objective is active for them.
 */
@Entry(
    "explore_biome_objective",
    "Objective completed by discovering one or more biomes",
    Colors.BLUE_VIOLET,
    icon = "mdi:map-marker-path"
)
class ExploreBiomeObjectiveEntry(
    override val id: String = "",
    override val name: String = "",
    override val children: List<Ref<out AudienceEntry>> = emptyList(),
    override val quest: Ref<QuestEntry> = emptyRef(),
    override val criteria: List<Criteria> = emptyList(),
    override val priorityOverride: Optional<Int> = Optional.empty(),

    @Colored
    @Placeholder
    @Help("The name to display to the player.")
    override val display: Var<String> = ConstVar("Explore the biome"),

    @Help("Biomes the player has to discover (e.g., 'typewriter:corrupted_forest')")
    val biomes: List<String> = emptyList(),

    @Help("Require every listed biome instead of any single one")
    val requireAll: Boolean = false,

    @Help(
        "Entries fired for the player when they enter one of the listed biomes and the objective is " +
            "complete, for example an action that sets the fact advancing the quest. They fire again " +
            "on each later entry while the objective stays active, so make them move the quest on."
    )
    val triggers: List<Ref<TriggerableEntry>> = emptyList(),

) : ObjectiveEntry {

    /** True once the player has visited what the objective asks for. */
    fun isComplete(player: Player): Boolean =
        BiomeExploration.isComplete(requiredKeys(), { BiomeDiscoveryService.hasDiscovered(player, it) }, requireAll)

    /** The listed biomes that resolve on this server; identifiers that do not resolve are ignored. */
    fun requiredKeys(): List<NamespacedKey> = biomes.mapNotNull { BiomeResolver.resolve(it)?.key }
}
