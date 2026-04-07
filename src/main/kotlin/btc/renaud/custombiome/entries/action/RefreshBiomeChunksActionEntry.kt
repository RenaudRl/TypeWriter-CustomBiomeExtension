package btc.renaud.custombiome.entries.action

import btc.renaud.custombiome.util.BiomePacketHelper
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

/**
 * Refresh biome chunk data for the player.
 * Useful after programmatic biome changes to update client visuals.
 */
@Entry(
    "refresh_biome_chunks_action",
    "Refresh biome data for chunks around the player",
    Colors.RED,
    icon = "mdi:refresh"
)
class RefreshBiomeChunksActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    
    @Help("Chunk radius to refresh around the player (default: 5)")
    val chunkRadius: Var<Int> = ConstVar(5),
    
) : ActionEntry {
    
    override fun ActionTrigger.execute() {
        val radius = chunkRadius.get(player, context) ?: 5
        BiomePacketHelper.refreshBiomesForPlayer(player, radius)
    }
}
