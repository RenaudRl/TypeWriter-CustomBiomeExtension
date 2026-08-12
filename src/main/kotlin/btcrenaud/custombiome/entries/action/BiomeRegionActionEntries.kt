package btcrenaud.custombiome.entries.action

import btcrenaud.custombiome.service.BiomePainter
import btcrenaud.custombiome.service.BiomeSnapshotService
import btcrenaud.custombiome.util.BiomeResolver
import btcrenaud.custombiome.util.WorldEditHandler
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
import btcrenaud.custombiome.util.Scheduling
import org.bukkit.entity.Player

/**
 * Paints a whole region, optionally recording what was there first.
 *
 * The region comes either from the player's WorldEdit selection or from a radius around them.
 * Positions are collected on the quart grid, the same grid the world actually stores, so a large
 * selection costs a fraction of what a per-block walk would.
 */
@Entry(
    "paint_biome_region_action",
    "Paint a biome over a region, with an optional snapshot to undo it later",
    Colors.RED,
    icon = "mdi:format-paint"
)
class PaintBiomeRegionActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),

    @Help("Biome identifier to paint (e.g., 'typewriter:corrupted_forest')")
    val biome: Var<String> = ConstVar("minecraft:plains"),

    @Help("Where the region comes from")
    val region: RegionSource = RegionSource.WORLDEDIT_SELECTION,

    @Help("Radius in blocks, used when the region is a radius around the player")
    val radius: Var<Int> = ConstVar(16),

    @Help("Snapshot name to record the previous biomes under. Leave empty to paint without an undo.")
    val snapshotId: String = "",

) : ActionEntry {

    override fun ActionTrigger.execute() {
        val identifier = biome.get(player, context)
        val target = BiomeResolver.resolve(identifier) ?: run {
            logger.warning("paint_biome_region_action: unknown biome '$identifier'")
            return
        }

        val positions = when (region) {
            RegionSource.WORLDEDIT_SELECTION -> selectionPositions(player) ?: run {
                logger.warning(
                    "paint_biome_region_action: ${player.name} has no WorldEdit selection " +
                        "(or WorldEdit is not installed)."
                )
                return
            }

            RegionSource.RADIUS_AROUND_PLAYER ->
                BiomePainter.radiusPositions(player.location, radius.get(player, context))
        }

        if (positions.isEmpty()) {
            logger.warning("paint_biome_region_action: the region covers nothing.")
            return
        }

        val world = player.world
        val snapshot = snapshotId.trim()

        // Reading the previous biomes touches many chunks, so it happens off the calling thread;
        // the painter re-schedules the writes onto the right region threads afterwards.
        Scheduling.runAsync {
            if (snapshot.isNotEmpty()) BiomeSnapshotService.capture(snapshot, world, positions)
            BiomePainter.paintPositions(world, target, positions)
        }
    }

    /** Walks the selection on the quart grid, keeping only the cells actually inside it. */
    private fun selectionPositions(player: Player): List<Triple<Int, Int, Int>>? {
        val selection = WorldEditHandler.getSelection(player) ?: return null
        val min = selection.minimumPoint
        val max = selection.maximumPoint

        val positions = mutableListOf<Triple<Int, Int, Int>>()
        var x = min.x()
        while (x <= max.x()) {
            var z = min.z()
            while (z <= max.z()) {
                var y = min.y()
                while (y <= max.y()) {
                    if (selection.contains(com.sk89q.worldedit.math.BlockVector3.at(x, y, z))) {
                        positions += Triple(x, y, z)
                    }
                    y += BiomePainter.QUART
                }
                z += BiomePainter.QUART
            }
            x += BiomePainter.QUART
        }
        return positions
    }

    enum class RegionSource {
        @Help("The triggering player's current WorldEdit selection")
        WORLDEDIT_SELECTION,

        @Help("A radius in blocks around the triggering player")
        RADIUS_AROUND_PLAYER,
    }
}

/**
 * Puts a region back the way a snapshot recorded it.
 */
@Entry(
    "restore_biome_action",
    "Restore biomes previously recorded by a paint snapshot",
    Colors.RED,
    icon = "mdi:undo-variant"
)
class RestoreBiomeActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),

    @Help("Snapshot name recorded by a paint action")
    val snapshotId: String = "",

    @Help("Delete the snapshot once it has been restored")
    val consume: Boolean = false,

) : ActionEntry {

    override fun ActionTrigger.execute() {
        val snapshot = snapshotId.trim()
        if (snapshot.isEmpty()) {
            logger.warning("restore_biome_action: no snapshot name configured.")
            return
        }

        Scheduling.runAsync {
            val restored = BiomeSnapshotService.restore(snapshot)
            if (restored == null) {
                logger.warning("restore_biome_action: snapshot '$snapshot' does not exist.")
                return@runAsync
            }
            if (consume) BiomeSnapshotService.delete(snapshot)
        }
    }
}
