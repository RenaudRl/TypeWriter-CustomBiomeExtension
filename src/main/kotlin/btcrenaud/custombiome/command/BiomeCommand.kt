package btcrenaud.custombiome.command

import btcrenaud.custombiome.entries.audience.BiomeRegionHolder
import btcrenaud.custombiome.registry.CustomBiomeRegistry
import btcrenaud.custombiome.service.BiomePainter
import btcrenaud.custombiome.text.BiomeListState
import btcrenaud.custombiome.text.BiomeTexts
import btcrenaud.custombiome.util.BiomePacketHelper
import btcrenaud.custombiome.util.BiomeResolver
import btcrenaud.custombiome.util.WorldEditHandler
import com.google.gson.JsonObject
import com.typewritermc.core.extension.annotations.TypewriterCommand
import com.typewritermc.engine.paper.command.dsl.*
import com.typewritermc.engine.paper.entry.StagingManager
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import com.typewritermc.engine.paper.utils.msg
import com.typewritermc.engine.paper.utils.sendMini
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.koin.java.KoinJavaComponent

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
                say(BiomeTexts.listEmpty)
                return@executes
            }

            show(BiomeTexts.listHeader(definitions.size))
            show(BiomeTexts.listStrategy(CustomBiomeRegistry.injector.unsupportedReason))

            for (definition in definitions.sortedBy { it.displayName }) {
                val key = definition.key
                val state = when {
                    CustomBiomeRegistry.resolveBiome(key) != null -> BiomeListState.LIVE
                    CustomBiomeRegistry.awaitingRestart(key) -> BiomeListState.WAITING_FOR_RESTART
                    else -> BiomeListState.NOT_REGISTERED
                }

                show(BiomeTexts.listEntry(definition.displayName, "$key", state, definition.temperature, definition.downfall))
            }
        }
    }

    literal("info") {
        withPermission("typewriter.biome.info")
        executePlayerOrTarget { target ->
            val biome = target.location.block.biome
            val definition = CustomBiomeRegistry.getDefinition(biome.key)

            show(BiomeTexts.infoHeader(target.name))
            show(BiomeTexts.infoName(BiomeResolver.readableName(biome)))
            show(BiomeTexts.infoId("${biome.key}"))
            show(if (definition != null) BiomeTexts.infoCustomYes else BiomeTexts.infoCustomNo)

            definition?.let {
                it.temperature?.let { value -> show(BiomeTexts.infoTemperature(value)) }
                it.downfall?.let { value -> show(BiomeTexts.infoDownfall(value)) }
                it.baseKey?.let { value -> show(BiomeTexts.infoBaseBiome("$value")) }
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

    literal("region") {
        withPermission("typewriter.biome.region")
        entry("entry", BiomeRegionHolder::class) { target ->
            executePlayer { player ->
                writeSelectionInto(player, target())
            }
        }
    }

    executes {
        show(BiomeTexts.help())
    }
}

/**
 * Replies go through snippets (see `BiomeTexts`), so an admin words them in the snippets file; the
 * placeholders of PlaceholderAPI are read for a player sender. [say] carries the Typewriter reply
 * prefix, [show] prints the text as it is (headers, lists).
 */
private fun ExecutionContext<CommandSourceStack>.say(text: String) = sender.msg(withPlaceholders(text))

private fun ExecutionContext<CommandSourceStack>.show(text: String) = sender.sendMini(withPlaceholders(text))

private fun ExecutionContext<CommandSourceStack>.withPlaceholders(text: String): String =
    (sender as? Player)?.let { text.parsePlaceholders(it) } ?: text

/**
 * Copies the sender's WorldEdit selection into the two corners of [target].
 *
 * WorldEdit stays a convenience: the same two fields can always be captured or typed from the
 * panel, so a server without WorldEdit loses this shortcut and nothing else.
 */
private fun ExecutionContext<CommandSourceStack>.writeSelectionInto(player: Player, target: BiomeRegionHolder) {
    val selection = runCatching { WorldEditHandler.getSelection(player) }.getOrNull()
    if (selection == null) {
        say(BiomeTexts.regionNoSelection)
        return
    }

    val worldName = player.world.name
    val staging = KoinJavaComponent.get<StagingManager>(StagingManager::class.java)
    val pageId = staging.findEntryPage(target.id).getOrNull()
    if (pageId == null) {
        say(BiomeTexts.regionPageNotFound(target.name))
        return
    }

    val min = selection.minimumPoint
    val max = selection.maximumPoint

    val failures = listOf(
        "cornerA" to position(worldName, min.x().toDouble(), min.y().toDouble(), min.z().toDouble()),
        "cornerB" to position(worldName, max.x().toDouble(), max.y().toDouble(), max.z().toDouble()),
    ).mapNotNull { (field, value) ->
        staging.updateEntryField(pageId, target.id, field, value).exceptionOrNull()?.message ?: return@mapNotNull null
    }

    if (failures.isNotEmpty()) {
        say(BiomeTexts.regionWriteFailed(failures))
        return
    }

    say(
        BiomeTexts.regionDone(
            entry = target.name,
            min = "${min.x()}, ${min.y()}, ${min.z()}",
            max = "${max.x()}, ${max.y()}, ${max.z()}",
            world = worldName,
        )
    )
    say(BiomeTexts.regionPublishHint)
}

/** The shape `PositionSerializer` reads back. */
private fun position(world: String, x: Double, y: Double, z: Double): JsonObject = JsonObject().apply {
    addProperty("world", world)
    addProperty("x", x)
    addProperty("y", y)
    addProperty("z", z)
    addProperty("yaw", 0f)
    addProperty("pitch", 0f)
}

/**
 * Only chunks this extension painted are re-sent, so "nothing to refresh" is a normal outcome and
 * has to read as one rather than as a silent success.
 */
private fun ExecutionContext<CommandSourceStack>.reportRefresh(target: Player, chunks: Int) {
    if (chunks == 0) {
        say(BiomeTexts.refreshNone(target.name))
        return
    }
    say(BiomeTexts.refreshDone(chunks, target.name))
}

private fun ExecutionContext<CommandSourceStack>.applyBiome(target: Player, biomeId: String, radius: Int) {
    val biome = BiomeResolver.resolve(biomeId)
    if (biome == null) {
        say(BiomeTexts.applyUnknown(biomeId))
        say(BiomeTexts.applyUnknownHint)
        return
    }

    val result = BiomePainter.paintRadius(target.location, biome, radius)
    if (result.isEmpty) {
        say(BiomeTexts.applyNothing)
        return
    }

    say(BiomeTexts.applyDone(BiomeResolver.readableName(biome), result.quartsWritten, result.chunksTouched))
}
