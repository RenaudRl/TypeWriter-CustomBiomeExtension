package btcrenaud.custombiome.command

import btcrenaud.custombiome.registry.CustomBiomeRegistry
import btcrenaud.custombiome.service.BiomePainter
import btcrenaud.custombiome.util.BiomePacketHelper
import btcrenaud.custombiome.util.BiomeResolver
import com.typewritermc.core.extension.annotations.TypewriterCommand
import com.typewritermc.engine.paper.command.dsl.*
import com.typewritermc.engine.paper.utils.msg
import com.typewritermc.engine.paper.utils.sendMini
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

/**
 * Operator commands for custom biomes.
 *
 * - `/tw biome list` — registered biomes and whether each one is live or waiting for a restart
 * - `/tw biome info [player]` — what biome a player is standing in
 * - `/tw biome apply <biome> [radius]` — paint a biome around a player
 * - `/tw biome refresh [radius]` — resend biome data to a player
 */
@TypewriterCommand
fun CommandTree.biomeCommand() = literal("biome") {
    withPermission("typewriter.biome")

    literal("list") {
        withPermission("typewriter.biome.list")
        executes {
            val definitions = CustomBiomeRegistry.allDefinitions()

            if (definitions.isEmpty()) {
                sender.msg("<yellow>No custom biomes registered.</yellow>")
                return@executes
            }

            sender.sendMini("\n<gradient:#00d4ff:#0099ff><b>Custom Biomes (${definitions.size})</b></gradient>")
            sender.sendMini("<dark_gray>Strategy: ${CustomBiomeRegistry.injector.describe}</dark_gray>\n")

            for (definition in definitions.sortedBy { it.displayName }) {
                val key = definition.key
                val live = CustomBiomeRegistry.resolveBiome(key) != null
                val waiting = CustomBiomeRegistry.awaitingRestart(key)

                val icon = when {
                    live -> "<#7ed957>•</#7ed957>"
                    waiting -> "<#ffcc00>⚠</#ffcc00>"
                    else -> "<#ff6b6b>✖</#ff6b6b>"
                }
                val status = when {
                    live -> "<green>Live in the registry</green>"
                    waiting -> "<yellow>Written to the datapack — needs a restart</yellow>"
                    else -> "<red>Not registered</red>"
                }

                val temp = definition.temperature?.let { " <gray>T:$it</gray>" } ?: ""
                val down = definition.downfall?.let { " <gray>D:$it</gray>" } ?: ""

                sender.sendMini(
                    "<hover:show_text:'$status\n<gray>Click to copy: $key</gray>'>" +
                        "<click:copy_to_clipboard:'$key'>$icon <white>${definition.displayName}</white> " +
                        "<#a0a0a0>($key)</#a0a0a0>$temp$down</click></hover>"
                )
            }
        }
    }

    literal("info") {
        withPermission("typewriter.biome.info")
        executePlayerOrTarget { target ->
            val biome = target.location.block.biome
            val definition = CustomBiomeRegistry.getDefinition(biome.key)

            sender.sendMini("\n<gradient:#00d4ff:#0099ff><b>Biome Info for ${target.name}</b></gradient>\n")
            sender.sendMini("<gray>Name:</gray> <white>${BiomeResolver.readableName(biome)}</white>")
            sender.sendMini("<gray>ID:</gray> <white>${biome.key}</white>")
            sender.sendMini(
                "<gray>Custom:</gray> " +
                    if (definition != null) "<green>Yes</green>" else "<gray>No</gray>"
            )

            definition?.let {
                it.temperature?.let { value -> sender.sendMini("<gray>Temperature:</gray> <white>$value</white>") }
                it.downfall?.let { value -> sender.sendMini("<gray>Downfall:</gray> <white>$value</white>") }
                it.baseKey?.let { value -> sender.sendMini("<gray>Base Biome:</gray> <white>$value</white>") }
            }
        }
    }

    literal("apply") {
        withPermission("typewriter.biome.apply")
        greedyString("arguments") { args ->
            executePlayerOrTarget { target ->
                val split = args().trim().split(" ")
                applyBiome(target, split[0], split.getOrNull(1)?.toIntOrNull() ?: 0)
            }
        }
    }

    literal("refresh") {
        withPermission("typewriter.biome.refresh")
        int("radius", 1, 16) { radius ->
            executePlayerOrTarget { target ->
                reportRefresh(target, BiomePacketHelper.refreshBiomesForPlayer(target, radius()))
            }
        }

        executePlayerOrTarget { target ->
            reportRefresh(target, BiomePacketHelper.refreshBiomesForPlayer(target))
        }
    }

    executes {
        sender.sendMini(
            """
            |
            |<gradient:#00d4ff:#0099ff><b>Custom Biome Commands</b></gradient>
            |
            |<white>/tw biome list</white> <gray>- List all custom biomes</gray>
            |<white>/tw biome info [player]</white> <gray>- Show current biome info</gray>
            |<white>/tw biome apply <biome> [radius]</white> <gray>- Paint a biome around a player</gray>
            |<white>/tw biome refresh [radius]</white> <gray>- Resend biome data</gray>
            |
            """.trimMargin()
        )
    }
}

/**
 * Only chunks this extension painted are re-sent, so "nothing to refresh" is a normal outcome and
 * has to read as one rather than as a silent success.
 */
private fun ExecutionContext<CommandSourceStack>.reportRefresh(target: Player, chunks: Int) {
    if (chunks == 0) {
        sender.msg("<yellow>No painted chunks near ${target.name} to refresh.</yellow>")
        return
    }
    sender.msg("Resent biome data for <green>$chunks</green> painted chunk(s) to ${target.name}.")
}

private fun ExecutionContext<CommandSourceStack>.applyBiome(target: Player, biomeId: String, radius: Int) {
    val biome = BiomeResolver.resolve(biomeId)
    if (biome == null) {
        sender.msg("<red>Unknown biome: $biomeId</red>")
        sender.msg("<gray>Use /tw biome list to see available custom biomes.</gray>")
        return
    }

    val result = BiomePainter.paintRadius(target.location, biome, radius)
    if (result.isEmpty) {
        sender.msg("<yellow>Nothing to paint at that location.</yellow>")
        return
    }

    sender.msg(
        "Painting <blue>${BiomeResolver.readableName(biome)}</blue> over " +
            "<green>${result.quartsWritten}</green> cell(s) across ${result.chunksTouched} chunk(s)."
    )
}
