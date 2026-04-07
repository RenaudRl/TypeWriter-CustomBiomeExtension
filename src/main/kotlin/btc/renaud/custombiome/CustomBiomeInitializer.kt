package btc.renaud.custombiome

import btc.renaud.custombiome.entries.event.EnterBiomeEventEntry
import btc.renaud.custombiome.entries.manifest.CustomBiomeDefinitionEntry
import btc.renaud.custombiome.placeholder.CustomBiomePlaceholders
import btc.renaud.custombiome.registry.CustomBiomeRegistry
import btc.renaud.custombiome.util.BiomeResolver
import com.typewritermc.core.entries.Query
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.extensions.placeholderapi.PlaceholderHandler
import com.typewritermc.engine.paper.plugin
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.context.GlobalContext
import org.koin.dsl.bind
import org.koin.dsl.module
import org.slf4j.LoggerFactory

/**
 * Initializer for the Custom Biome Extension.
 * Sets up the registry, placeholder handlers, and event listeners.
 */
@Singleton
class CustomBiomeInitializer : Initializable, Listener {
    
    private val logger = LoggerFactory.getLogger(CustomBiomeInitializer::class.java)
    
    private val customBiomeModule = module {
        single { CustomBiomeRegistry() }
        single { CustomBiomePlaceholders() } bind PlaceholderHandler::class
    }
    
    override suspend fun initialize() {
        Bukkit.getLogger().info("[CustomBiome] Initializing extension...")
        
        // Load Koin module
        GlobalContext.get().loadModules(listOf(customBiomeModule))
        
        // Initialize registry with datapack path
        val registry = GlobalContext.get().get<CustomBiomeRegistry>()
        registry.initialize(plugin.dataFolder.toPath())
        
        // Load and register all biome definitions from TypeWriter
        // This is crucial because ManifestEntry types are not auto-instantiated by TypeWriter
        val definitions = Query.find<CustomBiomeDefinitionEntry>().toList()
        Bukkit.getLogger().info("[CustomBiome] Found ${definitions.size} biome definitions in TypeWriter.")
        definitions.forEach { 
            it.register() 
        }
        
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin)
        
        // Initialize biome tracking for online players
        Bukkit.getOnlinePlayers().forEach { player ->
            EnterBiomeEventEntry.setLastBiome(player.uniqueId, player.location.block.biome)
        }
        
        logger.info("Custom Biome Extension initialized successfully")
    }
    
    override suspend fun shutdown() {
        logger.info("Shutting down Custom Biome Extension...")
        
        // Unregister listeners
        HandlerList.unregisterAll(this)
        
        // Clear biome tracking
        Bukkit.getOnlinePlayers().forEach { player ->
            EnterBiomeEventEntry.removePlayer(player.uniqueId)
        }
        
        // Clear resolver cache
        BiomeResolver.clearCache()
        
        // Prepare datapack before shutdown (in case definitions changed)
        // val registry = GlobalContext.get().get<CustomBiomeRegistry>()
        // registry.prepareDatapack()

        
        // Unload Koin module
        GlobalContext.get().unloadModules(listOf(customBiomeModule))
        
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
