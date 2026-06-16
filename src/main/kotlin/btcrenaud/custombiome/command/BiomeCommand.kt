package btcrenaud.custombiome.command

import btcrenaud.custombiome.registry.CustomBiomeRegistry
import btcrenaud.custombiome.util.BiomePacketHelper
import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.extension.annotations.TypewriterCommand
import com.typewritermc.engine.paper.command.dsl.*
import com.typewritermc.engine.paper.utils.msg
import com.typewritermc.engine.paper.utils.sendMini
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import btcrenaud.custombiome.nms.NMSHandler
import btcrenaud.custombiome.util.WorldEditHandler
import btcrenaud.custombiome.model.BiomeColors
import btcrenaud.custombiome.model.BiomeAttributes
import org.bukkit.NamespacedKey
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
            val registry: CustomBiomeRegistry = org.koin.java.KoinJavaComponent.get(CustomBiomeRegistry::class.java)
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
            val registry: CustomBiomeRegistry = org.koin.java.KoinJavaComponent.get(CustomBiomeRegistry::class.java)
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

                var sky: Int? = null
                var fog: Int? = null
                var water: Int? = null
                var waterFog: Int? = null
                var grass: Int? = null
                var foliage: Int? = null

                fun parseColor(s: String): Int? {
                    return try {
                        val clean = s.replace("#", "")
                        Integer.parseInt(clean, 16)
                    } catch (e: Exception) { null }
                }

                var i = 0
                while (i < args.size) {
                    val key = args[i].lowercase().replace(":", "")
                    val value = args.getOrNull(i + 1)?.let { parseColor(it) }

                    if (value != null) {
                        when (key) {
                            "sky" -> sky = value
                            "fog" -> fog = value
                            "water" -> water = value
                            "water_fog", "waterfog" -> waterFog = value
                            "grass" -> grass = value
                            "foliage" -> foliage = value
                        }
                        i += 2
                    } else {
                        val parts = args[i].split(":")
                        if (parts.size == 2) {
                            val k = parts[0].lowercase()
                            val v = parseColor(parts[1])
                            if (v != null) {
                                when (k) {
                                    "sky" -> sky = v
                                    "fog" -> fog = v
                                    "water" -> water = v
                                    "water_fog", "waterfog" -> waterFog = v
                                    "grass" -> grass = v
                                    "foliage" -> foliage = v
                                }
                            }
                        }
                        i++
                    }
                }

                val colors = BiomeColors(
                    sky = sky,
                    fog = fog,
                    water = water,
                    waterFog = waterFog,
                    grass = grass,
                    foliage = foliage
                )
                val attributes = BiomeAttributes()

                val randomKey = NamespacedKey("custombiome", "temp_${UUID.randomUUID().toString().substring(0, 8)}")
                sender.msg("<gray>Generating temporary biome: $randomKey...</gray>")

                try {
                    val nmsHandler = NMSHandler()
                    nmsHandler.createAndRegisterBiome(randomKey, colors, attributes)

                    @Suppress("DEPRECATION")
                    val biome = org.bukkit.Registry.BIOME.get(randomKey)

                    if (biome == null) {
                        sender.msg("<red>Failed to retrieve registered biome. NMS injection might have failed or Registry is desynced.</red>")
                        return@executePlayerOrTarget
                    }

                    val worldEditRegion = WorldEditHandler.getSelection(target)
                    val registry: CustomBiomeRegistry = org.koin.java.KoinJavaComponent.get(CustomBiomeRegistry::class.java)

                    if (worldEditRegion != null) {
                        sender.msg("<green>Applied to WorldEdit selection (${worldEditRegion.volume} blocks).</green>")

                        val world = target.world
                        val min = worldEditRegion.minimumPoint
                        val max = worldEditRegion.maximumPoint
                        val chunks = mutableSetOf<Pair<Int, Int>>()

                        for (x in min.x()..max.x() step 4) {
                            for (z in min.z()..max.z() step 4) {
                                for (y in min.y()..max.y() step 4) {
                                    if (worldEditRegion.contains(com.sk89q.worldedit.math.BlockVector3.at(x, y, z))) {
                                        world.setBiome(x, y, z, biome)
                                        chunks.add((x shr 4) to (z shr 4))
                                    }
                                }
                            }
                        }

                        BiomePacketHelper.sendBiomePackets(world, chunks)
                        sender.msg("<green>Biome updated!</green>")

                    } else {
                        sender.msg("<yellow>No WorldEdit selection. Applying to radius 10 around you.</yellow>")
                        registry.applyBiome(target.location, biome, 10)
                        sender.msg("<green>Biome updated!</green>")
                    }

                } catch (e: Exception) {
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

    val registry: CustomBiomeRegistry = org.koin.java.KoinJavaComponent.get(CustomBiomeRegistry::class.java)
    val affected = registry.applyBiome(target.location, biome, radius)

    val biomeName = BiomeResolver.readableName(biome)
    sender.msg("Applied <blue>$biomeName</blue> to <green>$affected</green> blocks at ${target.name}'s location.")
}
