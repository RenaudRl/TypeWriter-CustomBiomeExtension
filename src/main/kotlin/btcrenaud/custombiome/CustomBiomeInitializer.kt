package btcrenaud.custombiome

import btcrenaud.custombiome.entries.event.EnterBiomeEventEntry
import btcrenaud.custombiome.entries.manifest.CustomBiomeDefinitionEntry
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
class CustomBiomeInitializer : Initializable, Listener {

    private val logger = LoggerFactory.getLogger(CustomBiomeInitializer::class.java)

    private val registry: CustomBiomeRegistry by lazy {
        org.koin.java.KoinJavaComponent.get(CustomBiomeRegistry::class.java)
    }

    override suspend fun initialize() {
        logger.info("[CustomBiome] Initializing extension...")

        registry.initialize(plugin.dataFolder.toPath())

        val definitions = Query.find<CustomBiomeDefinitionEntry>().toList()
        logger.info("[CustomBiome] Found ${definitions.size} biome definitions in TypeWriter.")
        definitions.forEach { it.register(registry) }

        Bukkit.getPluginManager().registerEvents(this, plugin)

        Bukkit.getOnlinePlayers().forEach { player ->
            EnterBiomeEventEntry.setLastBiome(player.uniqueId, player.location.block.biome)
        }

        logger.info("Custom Biome Extension initialized successfully")
    }

    override suspend fun shutdown() {
        logger.info("Shutting down Custom Biome Extension...")

        HandlerList.unregisterAll(this)

        Bukkit.getOnlinePlayers().forEach { player ->
            EnterBiomeEventEntry.removePlayer(player.uniqueId)
        }

        BiomeResolver.clearCache()

        logger.info("Custom Biome Extension shutdown complete")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        EnterBiomeEventEntry.setLastBiome(player.uniqueId, player.location.block.biome)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        EnterBiomeEventEntry.removePlayer(event.player.uniqueId)
    }
}
