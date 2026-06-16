package btcrenaud.custombiome.service

import btcrenaud.custombiome.entries.manifest.CustomBiomeDefinitionEntry
import btcrenaud.custombiome.registry.CustomBiomeRegistry
import com.typewritermc.core.entries.Query
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.events.TypewriterUnloadEvent
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.slf4j.LoggerFactory
import java.util.Locale

/**
 * Service that manages the lifecycle of custom biome definitions.
 * Handles registration on init and cleanup on Typewriter unload/reload.
 */
@Singleton
class CustomBiomeLifecycleService : Listener {

    private val logger = LoggerFactory.getLogger(CustomBiomeLifecycleService::class.java)

    private val registry: CustomBiomeRegistry by lazy {
        org.koin.java.KoinJavaComponent.get(CustomBiomeRegistry::class.java)
    }

    @EventHandler
    fun onTypewriterUnload(event: TypewriterUnloadEvent) {
        logger.info("Typewriter unloading — unregistering all custom biomes")
        val definitions = Query.find<CustomBiomeDefinitionEntry>().toList()
        definitions.forEach { entry ->
            val normalizedId = entry.biomeId.lowercase(Locale.ENGLISH)
                .replace(' ', '_')
                .replace('-', '_')
            val key = NamespacedKey(entry.namespace.lowercase(Locale.ENGLISH), normalizedId)
            registry.unregisterDefinition(key)
        }
    }
}
