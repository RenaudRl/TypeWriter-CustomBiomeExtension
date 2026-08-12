package btcrenaud.custombiome.entries.action

import btcrenaud.custombiome.service.PlayerBiomeOverlayService
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
import com.typewritermc.engine.paper.logger

/**
 * Shows a biome to one player without changing the world.
 *
 * Nothing is written to disk and no other player is affected, so this is the right tool for
 * story-driven changes: a corrupted forest during a quest, a frozen village for one player, a
 * preview before committing a real paint.
 */
@Entry(
    "player_biome_overlay_action",
    "Show a biome to a single player without changing the world",
    Colors.RED,
    icon = "mdi:eye-circle"
)
class PlayerBiomeOverlayActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),

    @Help("What to do: show a biome to the player, or remove every overlay they have")
    val operation: Operation = Operation.APPLY,

    @Help("Biome identifier to show (e.g., 'typewriter:corrupted_forest'). Ignored when clearing.")
    val biome: Var<String> = ConstVar("minecraft:plains"),

    @Help("Chunk radius around the player the overlay covers")
    val chunkRadius: Var<Int> = ConstVar(5),

) : ActionEntry {

    override fun ActionTrigger.execute() {
        if (!PlayerBiomeOverlayService.isAvailable) {
            logger.warning("player_biome_overlay_action: PacketEvents is unavailable, overlay skipped.")
            return
        }

        if (operation == Operation.CLEAR) {
            PlayerBiomeOverlayService.clear(player)
            return
        }

        val identifier = biome.get(player, context)
        val key = BiomeResolver.resolve(identifier)?.key ?: run {
            logger.warning("player_biome_overlay_action: unknown biome '$identifier'")
            return
        }

        val radius = chunkRadius.get(player, context).coerceIn(0, 16)
        if (PlayerBiomeOverlayService.apply(player, key, radius) == null) {
            logger.warning(
                "player_biome_overlay_action: '$identifier' has no network id, " +
                    "so the client cannot be told about it."
            )
        }
    }

    enum class Operation {
        @Help("Show the configured biome to the player")
        APPLY,

        @Help("Remove every overlay and restore the real world")
        CLEAR,
    }
}
