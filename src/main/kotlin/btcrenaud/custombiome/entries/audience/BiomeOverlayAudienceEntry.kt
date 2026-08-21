package btcrenaud.custombiome.entries.audience

import btcrenaud.custombiome.entries.manifest.CustomBiomeDefinitionEntry
import btcrenaud.custombiome.service.PlayerBiomeOverlayService
import btcrenaud.custombiome.util.BiomeRegion
import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.entries.ref
import com.typewritermc.core.extension.annotations.ContentEditor
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.utils.point.Position
import com.typewritermc.engine.paper.content.modes.custom.PositionContentMode
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.engine.paper.logger
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Shows a biome to everyone in the audience, and gives the real world back on the way out.
 *
 * This is the audience-driven counterpart of `player_biome_overlay_action`. The action fires once
 * and has to be undone by hand; this follows the player's state instead. Placed under a
 * `criteria_audience`, a biome appears and disappears with a fact, a quest stage or any other
 * criterion, with nothing left to clean up.
 */
@Entry(
    "biome_overlay_audience",
    "Show a biome to the audience without changing the world",
    Colors.ORANGE,
    icon = "mdi:eye-circle-outline"
)
class BiomeOverlayAudienceEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("Custom biome to show, picked from its definition")
    val biome: Ref<CustomBiomeDefinitionEntry> = emptyRef(),

    @Help("Vanilla biome identifier, used only when no custom biome is picked. Example: 'minecraft:deep_dark'")
    val vanillaBiome: String = "",

    @Help("First corner of the region covered. Leave both corners empty to follow the player instead.")
    @ContentEditor(PositionContentMode::class)
    override val cornerA: Position = Position.ORIGIN,

    @Help("Opposite corner of the region covered")
    @ContentEditor(PositionContentMode::class)
    override val cornerB: Position = Position.ORIGIN,

    @Help("Chunk radius around the player, used only when no region is set")
    val chunkRadius: Var<Int> = ConstVar(5),

    @Help("Give the real biome back when the player leaves the audience")
    val restoreOnExit: Boolean = true,

) : AudienceEntry, BiomeRegionHolder {

    /** The biome to show: the referenced definition first, the free-text identifier as a fallback. */
    fun biomeKey(): NamespacedKey? {
        biome.get()?.let { return it.key }
        if (vanillaBiome.isBlank()) return null
        return BiomeResolver.resolve(vanillaBiome)?.key
    }

    override suspend fun display(): AudienceDisplay = BiomeOverlayAudienceDisplay(
        owner = ref().id,
        biome = biomeKey(),
        region = BiomeRegion.of(cornerA, cornerB),
        chunkRadius = chunkRadius,
        restoreOnExit = restoreOnExit,
        entryName = name,
    )
}

class BiomeOverlayAudienceDisplay(
    private val owner: String,
    private val biome: NamespacedKey?,
    private val region: BiomeRegion?,
    private val chunkRadius: Var<Int>,
    private val restoreOnExit: Boolean,
    private val entryName: String,
) : AudienceDisplay() {

    override fun onPlayerAdd(player: Player) {
        if (!PlayerBiomeOverlayService.isAvailable) {
            logger.warning("biome_overlay_audience '$entryName': PacketEvents is unavailable, nothing is shown.")
            return
        }

        val key = biome ?: run {
            logger.warning("biome_overlay_audience '$entryName': no biome configured.")
            return
        }

        val chunks = chunksFor(player) ?: return
        if (PlayerBiomeOverlayService.apply(player, key, chunks, owner) == null) {
            logger.warning(
                "biome_overlay_audience '$entryName': '$key' has no network id, " +
                    "so the client cannot be told about it."
            )
        }
    }

    override fun onPlayerRemove(player: Player) {
        if (!restoreOnExit) return
        PlayerBiomeOverlayService.clear(player, owner)
    }

    /**
     * The chunks this display covers: the configured region, or a radius around the player when no
     * region was captured.
     */
    private fun chunksFor(player: Player): Set<PlayerBiomeOverlayService.ChunkPos>? {
        val region = this.region
        if (region == null) {
            val world = player.world
            val radius = chunkRadius.get(player).coerceIn(0, 16)
            return PlayerBiomeOverlayService.chunksAround(
                world,
                player.location.blockX shr 4,
                player.location.blockZ shr 4,
                radius,
            )
        }

        // A stored world is normally a UUID, but a hand-written page may hold a name.
        val world = runCatching { Bukkit.getWorld(UUID.fromString(region.world)) }.getOrNull()
            ?: Bukkit.getWorld(region.world)
            ?: run {
            logger.warning("biome_overlay_audience '$entryName': world '${region.world}' is not loaded.")
            return null
        }

        return region.chunks().mapTo(mutableSetOf()) { (x, z) ->
            PlayerBiomeOverlayService.ChunkPos(world.uid, x, z)
        }
    }
}
