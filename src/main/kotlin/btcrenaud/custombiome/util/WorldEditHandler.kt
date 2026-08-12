package btcrenaud.custombiome.util

import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.regions.Region
import org.bukkit.entity.Player

object WorldEditHandler {

    fun getSelection(player: Player): Region? {
        val actor = BukkitAdapter.adapt(player)
        val session = WorldEdit.getInstance().sessionManager.get(actor)
        
        return try {
            session.getSelection(actor.world)
        } catch (e: IncompleteRegionException) {
            null
        } catch (e: Exception) {
            com.typewritermc.engine.paper.logger.log(java.util.logging.Level.WARNING, "Unhandled exception in WorldEditHandler", e)
            null
        }
    }
}
