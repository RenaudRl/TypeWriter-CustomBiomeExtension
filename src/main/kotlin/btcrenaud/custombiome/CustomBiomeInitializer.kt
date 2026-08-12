package btcrenaud.custombiome

import btcrenaud.custombiome.entries.manifest.CustomBiomeDefinitionEntry
import btcrenaud.custombiome.service.BiomeDiscoveryService
import btcrenaud.custombiome.service.BiomeTrackingService
import btcrenaud.custombiome.service.PaintedChunks
import btcrenaud.custombiome.service.PlayerBiomeOverlayService
import btcrenaud.custombiome.placeholder.CustomBiomePlaceholders
import btcrenaud.custombiome.registry.CustomBiomeRegistry
import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.entries.Query
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.plugin
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.slf4j.LoggerFactory

@Singleton
object CustomBiomeInitializer : Initializable, Listener {

    private val logger = LoggerFactory.getLogger(CustomBiomeInitializer::class.java)

    override suspend fun initialize() {
        Bukkit.getLogger().info("[CustomBiome] Initializing extension...")

        // The datapack has to live in the world folder, not in the plugin folder: the server only
        // reads world/datapacks.
        val worldFolder = Bukkit.getWorlds().firstOrNull()?.worldFolder?.toPath()
        if (worldFolder == null) {
            logger.error("No world is loaded; custom biomes cannot be registered.")
            return
        }
        CustomBiomeRegistry.initialize(worldFolder)

        val definitions = Query.find<CustomBiomeDefinitionEntry>().toList()
        Bukkit.getLogger().info("[CustomBiome] Found ${definitions.size} biome definitions in TypeWriter.")
        definitions.forEach { it.register() }

        Bukkit.getPluginManager().registerEvents(this, plugin)
        PlayerBiomeOverlayService.register()

        Bukkit.getOnlinePlayers().forEach { player ->
            BiomeTrackingService.prime(player, player.location.block.biome)
        }

        logger.info("Custom Biome Extension initialized successfully")
    }

    override suspend fun shutdown() {
        logger.info("Shutting down Custom Biome Extension...")

        HandlerList.unregisterAll(this)
        PlayerBiomeOverlayService.unregister()

        BiomeTrackingService.clear()
        BiomeDiscoveryService.clear()
        PaintedChunks.clear()
        BiomeResolver.clearCache()

        logger.info("Custom Biome Extension shutdown complete")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        BiomeTrackingService.prime(event.player, event.player.location.block.biome)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        BiomeTrackingService.forget(event.player.uniqueId)
        PlayerBiomeOverlayService.forget(event.player.uniqueId)
        BiomeDiscoveryService.unload(event.player.uniqueId)
    }
}
