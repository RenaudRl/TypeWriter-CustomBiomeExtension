package btcrenaud.custombiome.command

import btcrenaud.custombiome.registry.CustomBiomeRegistry
import btcrenaud.custombiome.util.BiomePacketHelper
import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.extension.annotations.TypewriterCommand
import com.typewritermc.engine.paper.command.dsl.*
import com.typewritermc.engine.paper.utils.FoliaScheduler
import com.typewritermc.engine.paper.utils.msg
import com.typewritermc.engine.paper.utils.sendMini
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import btcrenaud.custombiome.nms.NMSHandler
import btcrenaud.custombiome.util.WorldEditHandler
import btcrenaud.custombiome.model.BiomeColors
import btcrenaud.custombiome.model.BiomeAttributes
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Bukkit
import org.bukkit.block.Biome
import java.util.UUID

/**
 * TypeWriter commands for custom biomes.
 * 
 * Commands:
 * - /typewriter biome list - List all custom biomes
 * - /typewriter biome info - Show current biome info for player
 * - /typewriter biome apply <biome> [radius] - Apply biome to player's location
 * - /typewriter biome refresh [radius] - Refresh biome chunks for player
 */
@TypewriterCommand
fun CommandTree.biomeCommand() = literal("biome") {
    withPermission("typewriter.biome")
    
    // /tw biome list - List all custom biomes
    literal("list") {
        withPermission("typewriter.biome.list")
        executes {
            val registry = CustomBiomeRegistry
            val definitions = registry.allDefinitions()
            
            if (definitions.isEmpty()) {
                sender.msg("<yellow>No custom biomes registered.</yellow>")
                return@executes
            }
            
            sender.sendMini("\n<gradient:#00d4ff:#0099ff><b>Custom Biomes (${definitions.size})</b></gradient>\n")
            
            for (definition in definitions.sortedBy { it.displayName }) {
                val key = definition.key
                val biome = registry.resolveBiome(key)
                val isRegistered = biome != null
                
                val statusIcon = if (isRegistered) "<#7ed957>•</#7ed957>" else "<#ffcc00>⚠</#ffcc00>"
                val statusHover = if (isRegistered) "<green>Registered and active</green>" else "<yellow>Pending restart (not registered in server)</yellow>"
                
                val temp = definition.temperature?.let { " <gray>T:$it</gray>" } ?: ""
                val down = definition.downfall?.let { " <gray>D:$it</gray>" } ?: ""
                
                sender.sendMini("<hover:show_text:'$statusHover\n<gray>Click to copy: $key</gray>'><click:copy_to_clipboard:'$key'>$statusIcon <white>${definition.displayName}</white> <#a0a0a0>($key)</a0>$temp$down</click></hover>")
            }
        }
    }
    
    // /tw biome info [player] - Show current biome info
    literal("info") {
        withPermission("typewriter.biome.info")
        executePlayerOrTarget { target ->
            val biome = target.location.block.biome
            val key = biome.key.toString()
            val isCustom = BiomeResolver.isCustomBiome(biome)
            val registry = CustomBiomeRegistry
            val definition = registry.getDefinition(biome.key)
            
            sender.sendMini("\n<gradient:#00d4ff:#0099ff><b>Biome Info for ${target.name}</b></gradient>\n")
            sender.sendMini("<gray>Name:</gray> <white>${BiomeResolver.readableName(biome)}</white>")
            sender.sendMini("<gray>ID:</gray> <white>$key</white>")
            sender.sendMini("<gray>Custom:</gray> ${if (isCustom) "<green>Yes</green>" else "<gray>No</gray>"}")
            
            if (definition != null) {
                definition.temperature?.let { sender.sendMini("<gray>Temperature:</gray> <white>$it</white>") }
                definition.downfall?.let { sender.sendMini("<gray>Downfall:</gray> <white>$it</white>") }
                definition.baseKey?.let { sender.sendMini("<gray>Base Biome:</gray> <white>$it</white>") }
            }
        }
    }
    
    // /tw biome apply <biome> [radius] - Apply biome to location
    literal("apply") {
        withPermission("typewriter.biome.apply")
        greedyString("arguments") { args ->
            executePlayerOrTarget { target ->
                val split = args().split(" ")
                val biomeArg = split[0]
                val radiusArg = split.getOrNull(1)?.toIntOrNull() ?: 0
                applyBiome(target, biomeArg, radiusArg)
            }
        }
    }
    
    // /tw biome setcolor <type> <hex> [type:hex...] - Test biome colors
    literal("setcolor") {
        withPermission("typewriter.biome.setcolor")
        greedyString("args") { argsStr ->
            executePlayerOrTarget { target ->
                val args = argsStr().split(" ")
                if (args.size < 2) {
                    sender.msg("<red>Usage: /tw biome setcolor <type> <hex> [type:hex...]</red>")
                    sender.msg("<gray>Types: sky, fog, water, water_fog, grass, foliage, etc.</gray>")
                    return@executePlayerOrTarget
                }

                // 1. Normalise "<key> <value>" and "<key>:<value>" into one map, so each
                // key is interpreted in exactly one place.
                val values = mutableMapOf<String, String>()
                var i = 0
                while (i < args.size) {
                    val token = args[i]
                    val inlineParts = token.split(":", limit = 2)
                    if (inlineParts.size == 2 && inlineParts[1].isNotEmpty()) {
                        values[inlineParts[0].lowercase()] = inlineParts[1]
                        i++
                    } else {
                        val next = args.getOrNull(i + 1)
                        if (next == null) {
                            sender.msg("<red>Missing value for <white>${token}</white>.</red>")
                            return@executePlayerOrTarget
                        }
                        values[token.lowercase().removeSuffix(":")] = next
                        i += 2
                    }
                }

                fun parseColor(key: String): Int? = values[key]?.let {
                    Integer.parseInt(it.removePrefix("#"), 16)
                }

                val badClimate = mutableListOf<String>()
                fun parseClimate(vararg keys: String): Double? {
                    for (key in keys) {
                        val raw = values[key] ?: continue
                        val parsed = raw.toDoubleOrNull()
                        if (parsed == null) badClimate += "$key=$raw"
                        return parsed
                    }
                    return null
                }

                val colors = try {
                    BiomeColors(
                        sky = parseColor("sky"),
                        fog = parseColor("fog"),
                        water = parseColor("water"),
                        waterFog = parseColor("water_fog") ?: parseColor("waterfog"),
                        grass = parseColor("grass"),
                        foliage = parseColor("foliage"),
                    )
                } catch (e: NumberFormatException) {
                    sender.msg("<red>Invalid hex colour: ${e.message}</red>")
                    return@executePlayerOrTarget
                }

                // Left empty, these inherit from the base biome (see NMSHandler).
                val temperature = parseClimate("temperature", "temp")
                val downfall = parseClimate("downfall", "humidity")
                if (badClimate.isNotEmpty()) {
                    sender.msg("<red>Expected a number for: <white>${badClimate.joinToString(", ")}</white>.</red>")
                    return@executePlayerOrTarget
                }

                val attributes = BiomeAttributes()

                // 3. Create & Register Biome
                val randomKey = NamespacedKey("custombiome", "temp_${UUID.randomUUID().toString().substring(0, 8)}")
                sender.msg("<gray>Generating temporary biome: $randomKey...</gray>")
                
                try {
                    val nmsHandler = NMSHandler() // Instantiate here
                    nmsHandler.createAndRegisterBiome(randomKey, colors, attributes, temperature, downfall)
                    
                    // 4. Retrieve Bukkit Biome — registered just above, so the registry
                    // should now serve it unless the NMS injection silently failed.
                     @Suppress("DEPRECATION")
                    val biome = org.bukkit.Registry.BIOME.get(randomKey)
                     
                     if (biome == null) {
                         sender.msg("<red>Failed to retrieve registered biome. NMS injection might have failed or Registry is desynced.</red>")
                         return@executePlayerOrTarget
                     }

                    // 5. Apply to Region
                    val worldEditRegion = WorldEditHandler.getSelection(target)
                    val registry = CustomBiomeRegistry
                    
                    if (worldEditRegion != null) {
                        sender.msg("<green>Applied to WorldEdit selection (${worldEditRegion.volume} blocks).</green>")
                        // CustomBiomeRegistry.applyBiome works on a location + radius, so the
                        // selection is walked here instead.

                        val world = target.world
                        val min = worldEditRegion.minimumPoint
                        val max = worldEditRegion.maximumPoint

                        // Biomes are stored per 4x4x4 quart, so stepping by 4 covers the
                        // selection without writing the same quart repeatedly.
                        val byChunk = HashMap<Pair<Int, Int>, MutableList<com.sk89q.worldedit.math.BlockVector3>>()
                        for (x in min.x()..max.x() step 4) {
                            for (z in min.z()..max.z() step 4) {
                                for (y in min.y()..max.y() step 4) {
                                    val point = com.sk89q.worldedit.math.BlockVector3.at(x, y, z)
                                    if (!worldEditRegion.contains(point)) continue
                                    byChunk.getOrPut((x shr 4) to (z shr 4)) { mutableListOf() }.add(point)
                                }
                            }
                        }

                        // One task per chunk, on the thread owning it: on Folia a selection
                        // spanning several regions cannot be written from a single thread,
                        // and this keeps a huge selection off one long blocking pass.
                        byChunk.forEach { (chunk, points) ->
                            val anchor = Location(
                                world,
                                (chunk.first shl 4).toDouble(),
                                world.minHeight.toDouble(),
                                (chunk.second shl 4).toDouble(),
                            )
                            FoliaScheduler.runAtLocation(anchor) {
                                points.forEach { world.setBiome(it.x(), it.y(), it.z(), biome) }
                                BiomePacketHelper.sendBiomePackets(world, setOf(chunk))
                            }
                        }
                        sender.msg("<green>Biome update queued for ${byChunk.size} chunk(s).</green>")

                    } else {
                        // Fallback to radius 10
                        sender.msg("<yellow>No WorldEdit selection. Applying to radius 10 around you.</yellow>")
                        registry.applyBiome(target.location, biome, 10)
                        sender.msg("<green>Biome updated!</green>")
                    }

                } catch (e: Exception) {
                    com.typewritermc.engine.paper.logger.log(java.util.logging.Level.WARNING, "Unhandled exception in BiomeCommand", e)
                    sender.msg("<red>Error generating biome: ${e.message}</red>")
                }
            }
        }
    }

    // /tw biome refresh [radius] [player] - Refresh biome chunks
    literal("refresh") {
        withPermission("typewriter.biome.refresh")
        int("radius", 1, 16) { radiusArg ->
            executePlayerOrTarget { target ->
                BiomePacketHelper.refreshBiomesForPlayer(target, radiusArg())
                sender.msg("Refreshed biome chunks for <green>${target.name}</green> (radius: ${radiusArg()}).")
            }
        }
        
        // Default radius 5
        executePlayerOrTarget { target ->
            BiomePacketHelper.refreshBiomesForPlayer(target, 5)
            sender.msg("Refreshed biome chunks for <green>${target.name}</green>.")
        }
    }
    
    // Default - show help
    executes {
        sender.sendMini("""
            |
            |<gradient:#00d4ff:#0099ff><b>Custom Biome Commands</b></gradient>
            |
            |<white>/tw biome list</white> <gray>- List all custom biomes</gray>
            |<white>/tw biome info [player]</white> <gray>- Show current biome info</gray>
            |<white>/tw biome apply <biome> [radius]</white> <gray>- Apply biome to location</gray>
            |<white>/tw biome refresh [radius]</white> <gray>- Refresh biome chunks</gray>
            |
        """.trimMargin())
    }
}

private fun ExecutionContext<CommandSourceStack>.applyBiome(target: Player, biomeId: String, radius: Int) {
    val biome = BiomeResolver.resolve(biomeId)
    if (biome == null) {
        sender.msg("<red>Unknown biome: $biomeId</red>")
        sender.msg("<gray>Use /tw biome list to see available custom biomes.</gray>")
        return
    }
    
    val registry = CustomBiomeRegistry
    val affected = registry.applyBiome(target.location, biome, radius)
    
    val biomeName = BiomeResolver.readableName(biome)
    sender.msg("Applied <blue>$biomeName</blue> to <green>$affected</green> blocks at ${target.name}'s location.")
}
