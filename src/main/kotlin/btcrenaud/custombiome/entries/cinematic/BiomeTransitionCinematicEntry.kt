package btcrenaud.custombiome.entries.cinematic

import btcrenaud.custombiome.service.PlayerBiomeOverlayService
import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Segments
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.entries.CinematicAction
import com.typewritermc.engine.paper.entry.entries.CinematicEntry
import com.typewritermc.engine.paper.entry.entries.Segment
import com.typewritermc.engine.paper.entry.entries.activeSegmentAt
import com.typewritermc.engine.paper.entry.entries.canFinishAt
import com.typewritermc.engine.paper.logger
import org.bukkit.entity.Player

/**
 * Changes the biome a player sees over the course of a cinematic.
 *
 * Each segment shows one biome to that player alone, through the per-player overlay, so the world
 * is never touched and other players see nothing. When the cinematic ends the overlay is dropped
 * and the real world comes back.
 *
 * This sequences biomes; it does not interpolate between them. Minecraft resolves biome colours
 * from the registry, so a genuine fade would require pre-registering one biome per intermediate
 * step — chain several short segments to approximate it.
 */
@Entry(
    "biome_transition_cinematic",
    "Change the biome a player sees during a cinematic",
    Colors.CYAN,
    icon = "mdi:transition"
)
class BiomeTransitionCinematicEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),

    @Help("Chunk radius around the player each segment covers")
    val chunkRadius: Int = 5,

    @Segments(Colors.CYAN, "mdi:pine-tree")
    @Help("The biomes to show, in order")
    val segments: List<BiomeTransitionSegment> = emptyList(),

) : CinematicEntry {

    override fun create(player: Player): CinematicAction =
        BiomeTransitionCinematicAction(player, this)
}

data class BiomeTransitionSegment(
    override val startFrame: Int = 0,
    override val endFrame: Int = 0,

    @Help("Biome identifier to show for this segment (e.g., 'typewriter:corrupted_forest')")
    val biome: String = "minecraft:plains",
) : Segment

private class BiomeTransitionCinematicAction(
    private val player: Player,
    private val entry: BiomeTransitionCinematicEntry,
) : CinematicAction {

    /** Avoids resending the same overlay on every frame of a segment. */
    private var shown: String? = null

    override suspend fun setup() {
        if (!PlayerBiomeOverlayService.isAvailable) {
            logger.warning("biome_transition_cinematic: PacketEvents is unavailable, no biome will change.")
        }
    }

    override suspend fun tick(frame: Int) {
        val segment = entry.segments activeSegmentAt frame ?: return
        if (segment.biome == shown) return

        val key = BiomeResolver.resolve(segment.biome)?.key ?: run {
            logger.warning("biome_transition_cinematic: unknown biome '${segment.biome}'")
            return
        }

        PlayerBiomeOverlayService.apply(player, key, entry.chunkRadius)
        shown = segment.biome
    }

    override suspend fun teardown() {
        PlayerBiomeOverlayService.clear(player)
        shown = null
    }

    override fun canFinish(frame: Int): Boolean = entry.segments canFinishAt frame
}
