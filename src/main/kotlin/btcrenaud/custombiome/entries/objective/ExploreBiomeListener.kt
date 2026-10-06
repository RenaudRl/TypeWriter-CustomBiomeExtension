package btcrenaud.custombiome.entries.objective

import btcrenaud.custombiome.entries.event.crossedQuart
import btcrenaud.custombiome.service.BiomeTrackingService
import com.typewritermc.core.entries.ref
import com.typewritermc.core.interaction.context
import com.typewritermc.engine.paper.entry.inAudience
import com.typewritermc.engine.paper.entry.triggerEntriesFor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * Evaluates the explore-biome objectives when a player crosses into a new biome.
 *
 * It is a plain Bukkit listener owned by the extension lifecycle, not an `@EntryListener`: the
 * engine only wires entry listeners whose entry is an `EventEntry`, and an objective is not one.
 * Only a change of biome can complete an objective, and biomes are stored per 4x4x4 cell, so the move
 * is dropped before anything else unless a cell boundary was crossed. It also feeds the discovery
 * record, which is what lets an objective work on a page without any enter/leave biome event.
 * It runs on the region thread owning the player; firing the triggers only queues them.
 */
class ExploreBiomeListener(
    private val objectives: List<ExploreBiomeObjectiveEntry>,
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!event.crossedQuart()) return

        val player = event.player
        val entered = BiomeTrackingService.observe(player, event.to.block.biome, event)?.to?.key ?: return

        objectives
            .filter { entered in it.requiredKeys() }
            .filter { it.isComplete(player) && player.inAudience(it.ref()) }
            .forEach { it.triggers.triggerEntriesFor(player, context()) }
    }

    companion object {
        /**
         * Starts listening for [objectives]; returns `null` and registers nothing when there are none,
         * so a server without explore-biome objectives pays no cost on player movement.
         */
        fun register(pluginManager: PluginManager, plugin: Plugin, objectives: List<ExploreBiomeObjectiveEntry>): ExploreBiomeListener? {
            if (objectives.isEmpty()) return null
            return ExploreBiomeListener(objectives).also { pluginManager.registerEvents(it, plugin) }
        }

        fun unregister(listener: ExploreBiomeListener?) {
            listener?.let { HandlerList.unregisterAll(it) }
        }
    }
}
