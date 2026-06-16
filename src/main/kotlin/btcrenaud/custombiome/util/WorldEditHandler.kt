package btcrenaud.custombiome.util

import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.regions.Region
import org.bukkit.entity.Player
import org.slf4j.LoggerFactory

object WorldEditHandler {

    private val logger = LoggerFactory.getLogger(WorldEditHandler::class.java)

    fun getSelection(player: Player): Region? {
        val actor = BukkitAdapter.adapt(player)
        val session = WorldEdit.getInstance().sessionManager.get(actor)

        return try {
            session.getSelection(actor.world)
        } catch (e: IncompleteRegionException) {
            null
        } catch (e: Exception) {
            logger.warn("Failed to get WorldEdit selection for ${player.name}: ${e.message}")
            null
        }
    }
}
