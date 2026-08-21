package btcrenaud.custombiome.entries.audience

import btcrenaud.custombiome.util.BiomeRegion
import org.bukkit.Location
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.ref
import com.typewritermc.core.extension.annotations.ContentEditor
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.utils.point.Position
import com.typewritermc.engine.paper.content.modes.custom.PositionContentMode
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.AudienceFilter
import com.typewritermc.engine.paper.entry.entries.AudienceFilterEntry
import com.typewritermc.engine.paper.entry.entries.Invertible
import com.typewritermc.engine.paper.entry.include
import com.typewritermc.engine.paper.entry.literal
import com.typewritermc.engine.paper.entry.placeholderParser
import com.typewritermc.engine.paper.entry.supplyPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent

/** Bridges a Bukkit location onto the world-agnostic region check. */
internal fun BiomeRegion.contains(location: Location, ignoreY: Boolean): Boolean {
    val world = location.world ?: return false
    if (!matchesWorld(world.uid.toString(), world.name)) return false
    return contains(this.world, location.blockX, location.blockY, location.blockZ, ignoreY)
}

/**
 * Keeps the players standing inside a region defined by two captured corners.
 *
 * This entry decides nothing about biomes: it is a door. Its children are what act — a
 * [BiomeOverlayAudienceEntry] to show a biome, or any other audience.
 */
@Entry(
    "biome_region_audience",
    "Keep the players standing inside a region defined by two captured corners",
    Colors.GREEN,
    icon = "mdi:selection-marker"
)
class BiomeRegionAudienceEntry(
    override val id: String = "",
    override val name: String = "",
    override val children: List<Ref<out AudienceEntry>> = emptyList(),

    @Help("First corner of the region. Capture it in game, or use /tw biome region.")
    @ContentEditor(PositionContentMode::class)
    override val cornerA: Position = Position.ORIGIN,

    @Help("Opposite corner. Must be in the same world as the first one.")
    @ContentEditor(PositionContentMode::class)
    override val cornerB: Position = Position.ORIGIN,

    @Help("Ignore height, turning the region into an infinite column")
    val ignoreY: Boolean = false,

    override val inverted: Boolean = false,

) : AudienceFilterEntry, Invertible, BiomeRegionHolder {

    /** Null when a corner was never captured, or when the two are in different worlds. */
    val region: BiomeRegion? get() = BiomeRegion.of(cornerA, cornerB)

    override suspend fun display(): AudienceFilter = BiomeRegionAudienceFilter(ref(), region, ignoreY)

    override fun parser() = placeholderParser {
        include(super.parser())
        literal("in_region") {
            supplyPlayer { player ->
                (region?.contains(player.location, ignoreY) == true).toString()
            }
        }
        literal("region_name") {
            supplyPlayer { _ -> name }
        }
    }
}

class BiomeRegionAudienceFilter(
    ref: Ref<out AudienceFilterEntry>,
    private val region: BiomeRegion?,
    private val ignoreY: Boolean,
) : AudienceFilter(ref) {

    override fun filter(player: Player): Boolean =
        region?.contains(player.location, ignoreY) == true

    // Without these the filter would be evaluated once, when the player enters the parent audience,
    // and never again — the player could walk across the whole map without the audience noticing.
    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if (!event.hasChangedBlock()) return
        // The destination, not the current location: the move has not been applied yet.
        event.player.updateFilter(region?.contains(event.to, ignoreY) == true)
    }

    // A teleport carries its own handler list, so the move listener above never sees it.
    @EventHandler
    fun onTeleport(event: PlayerTeleportEvent) {
        val destination = event.to
        event.player.updateFilter(region?.contains(destination, ignoreY) == true)
    }

    @EventHandler
    fun onChangedWorld(event: PlayerChangedWorldEvent) {
        event.player.refresh()
    }
}
