package btcrenaud.custombiome.entries.action

import btcrenaud.custombiome.registry.CustomBiomeRegistry
import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.entry.entries.get
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * Apply a biome to the player's current location with an optional radius.
 */
@Entry(
    "apply_biome_action",
    "Apply a biome to a location with optional radius",
    Colors.BLUE,
    icon = "mdi:pine-tree"
)
class ApplyBiomeActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),

    @Help("Biome identifier (e.g., 'minecraft:plains' or 'typewriter:my_biome')")
    val biome: Var<String> = ConstVar("minecraft:plains"),

    @Help("Radius in blocks around the player's location (0 = single column)")
    val radius: Var<Int> = ConstVar(0),

    @Help("If true, applies at player's location. If false, requires a target location.")
    val atPlayerLocation: Boolean = true,

) : ActionEntry {

    override fun ActionTrigger.execute() {
        val biomeId = biome.get(player, context)
        val blockRadius = radius.get(player, context)

        val resolvedBiome = BiomeResolver.resolve(biomeId) ?: run {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown biome: $biomeId</red>"))
            return
        }

        val location = if (atPlayerLocation) {
            player.location
        } else {
            player.location // Could be extended to support target locations
        }

        val registry: CustomBiomeRegistry = org.koin.java.KoinJavaComponent.get(CustomBiomeRegistry::class.java)
        val affected = registry.applyBiome(location, resolvedBiome, blockRadius)

        if (affected > 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Applied biome ${BiomeResolver.readableName(resolvedBiome)} to $affected blocks.</green>"))
        }
    }
}
