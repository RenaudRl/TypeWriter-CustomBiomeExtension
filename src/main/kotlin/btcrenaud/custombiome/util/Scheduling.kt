package btcrenaud.custombiome.util

import com.typewritermc.engine.paper.plugin
import org.bukkit.Bukkit
import org.bukkit.Location

/**
 * Scheduling that works on both Paper and Folia.
 *
 * Paper runs everything on one main thread; Folia owns each region on its own thread and refuses
 * writes made from anywhere else. Biome writes therefore have to run on the thread that owns the
 * target chunk, which is what [runAtLocation] guarantees on either platform.
 */
object Scheduling {

    private val isFolia: Boolean = runCatching {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        true
    }.getOrDefault(false)

    /** Runs [block] on the thread that owns [location]. */
    fun runAtLocation(location: Location, block: () -> Unit) {
        if (isFolia) {
            Bukkit.getRegionScheduler().execute(plugin, location, block)
            return
        }
        Bukkit.getScheduler().runTask(plugin, block)
    }

    /** Runs [block] off the main thread, for work that only reads or touches the filesystem. */
    fun runAsync(block: () -> Unit) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin) { block() }
            return
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, block)
    }

    /** Runs [block] once, [delayTicks] ticks from now, on the global/main thread. */
    fun runLater(delayTicks: Long, block: () -> Unit) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { block() }, delayTicks)
            return
        }
        Bukkit.getScheduler().runTaskLater(plugin, block, delayTicks)
    }
}
