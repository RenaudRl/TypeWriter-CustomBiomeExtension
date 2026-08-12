package btcrenaud.custombiome.service

import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.extension.annotations.Help
import org.bukkit.block.Biome
import org.bukkit.entity.Player

/** Which biome a check should consider for a player. */
enum class BiomeSource {
    @Help("What the player is actually being shown, including any per-player overlay")
    PLAYER_VIEW,

    @Help("What the world really contains, ignoring per-player overlays")
    REAL_WORLD,
}

/**
 * Single place that answers "which biome is this player in".
 *
 * Overlays would otherwise create a contradiction: a player shown a frozen wasteland would keep
 * triggering the plains quests underneath. Defaulting to [BiomeSource.PLAYER_VIEW] makes what the
 * player sees drive the logic, while [BiomeSource.REAL_WORLD] stays available for the cases where
 * the actual geography is what matters, such as region protection.
 */
object BiomeView {

    fun biomeOf(player: Player, source: BiomeSource = BiomeSource.PLAYER_VIEW): Biome {
        val real = player.location.block.biome
        if (source == BiomeSource.REAL_WORLD) return real

        val location = player.location
        val overlay = PlayerBiomeOverlayService.overlayAt(player, player.world, location.blockX, location.blockZ)
            ?: return real

        return BiomeResolver.resolve(overlay.toString()) ?: real
    }
}
