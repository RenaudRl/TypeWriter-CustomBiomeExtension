package btcrenaud.custombiome.text

import com.typewritermc.engine.paper.snippets.snippet

// Everything the `/tw biome ...` commands and the biome placeholders print in game is a Typewriter
// snippet: the server owner edits it in the snippets file and nothing is fixed in the code. The values
// in `BiomeTextDefaults` are what a snippet starts from. Only the server console log stays in the code.

private const val LIST = "content.custombiome.command.list."
private const val INFO = "content.custombiome.command.info."
private const val HELP = "content.custombiome.command.help."
private const val REGION = "content.custombiome.command.region."
private const val REFRESH = "content.custombiome.command.refresh."
private const val APPLY = "content.custombiome.command.apply."
private const val PLACEHOLDER = "content.custombiome.placeholder."

private val listEmptyText by snippet(
    LIST + "empty", BiomeTextDefaults.LIST_EMPTY,
    "Reply of /tw biome list when no custom biome is registered.",
)
private val listHeaderText by snippet(
    LIST + "header", BiomeTextDefaults.LIST_HEADER,
    "First line of /tw biome list. {count} is the number of registered custom biomes.",
)
private val listStrategyText by snippet(
    LIST + "strategy", BiomeTextDefaults.LIST_STRATEGY,
    "Second line of /tw biome list. {strategy} is how biomes get registered, worded by the two strategy snippets below.",
)
private val strategyLiveText by snippet(
    LIST + "strategy_live", BiomeTextDefaults.STRATEGY_LIVE,
    "The strategy when biomes are injected into the live registry of the server.",
)
private val strategyUnsupportedText by snippet(
    LIST + "strategy_unsupported", BiomeTextDefaults.STRATEGY_UNSUPPORTED,
    "The strategy when the server does not allow live injection (biomes then need a restart). {reason} is the technical cause, in English.",
)
private val iconLiveText by snippet(
    LIST + "icon_live", BiomeTextDefaults.LIST_ICON_LIVE,
    "Icon in front of a biome that is live in the registry. MiniMessage.",
)
private val iconWaitingText by snippet(
    LIST + "icon_waiting", BiomeTextDefaults.LIST_ICON_WAITING,
    "Icon in front of a biome written to the datapack that waits for a restart. MiniMessage.",
)
private val iconMissingText by snippet(
    LIST + "icon_missing", BiomeTextDefaults.LIST_ICON_MISSING,
    "Icon in front of a biome that is not registered at all. MiniMessage.",
)
private val statusLiveText by snippet(
    LIST + "status_live", BiomeTextDefaults.LIST_STATUS_LIVE,
    "Hover text of a live biome in /tw biome list. It sits inside a single-quoted MiniMessage argument: do not use a single quote in it.",
)
private val statusWaitingText by snippet(
    LIST + "status_waiting", BiomeTextDefaults.LIST_STATUS_WAITING,
    "Hover text of a biome that waits for a restart. Same rule: no single quote.",
)
private val statusMissingText by snippet(
    LIST + "status_missing", BiomeTextDefaults.LIST_STATUS_MISSING,
    "Hover text of a biome that is not registered. Same rule: no single quote.",
)
private val listEntryText by snippet(
    LIST + "entry", BiomeTextDefaults.LIST_ENTRY,
    "One line per biome in /tw biome list. {icon} and {status} are the icon and hover snippets above, {name} the display name, {key} the biome key (clicking copies it), {temperature} and {downfall} the two snippets below (empty when the biome sets none).",
)
private val listTemperatureText by snippet(
    LIST + "temperature", BiomeTextDefaults.LIST_TEMPERATURE,
    "Temperature part of a biome line, only for a biome that sets one. {value} is the temperature.",
)
private val listDownfallText by snippet(
    LIST + "downfall", BiomeTextDefaults.LIST_DOWNFALL,
    "Downfall part of a biome line, only for a biome that sets one. {value} is the downfall.",
)

private val infoHeaderText by snippet(
    INFO + "header", BiomeTextDefaults.INFO_HEADER,
    "First line of /tw biome info. {player} is the inspected player.",
)
private val infoNameText by snippet(
    INFO + "name", BiomeTextDefaults.INFO_NAME,
    "Name line of /tw biome info. {value} is the readable biome name.",
)
private val infoIdText by snippet(
    INFO + "id", BiomeTextDefaults.INFO_ID,
    "ID line of /tw biome info. {value} is the biome key.",
)
private val infoCustomYesText by snippet(
    INFO + "custom_yes", BiomeTextDefaults.INFO_CUSTOM_YES,
    "Custom line of /tw biome info when the biome is a custom one.",
)
private val infoCustomNoText by snippet(
    INFO + "custom_no", BiomeTextDefaults.INFO_CUSTOM_NO,
    "Custom line of /tw biome info when the biome is a vanilla one.",
)
private val infoTemperatureText by snippet(
    INFO + "temperature", BiomeTextDefaults.INFO_TEMPERATURE,
    "Temperature line of /tw biome info, only for a custom biome that sets one. {value} is the temperature.",
)
private val infoDownfallText by snippet(
    INFO + "downfall", BiomeTextDefaults.INFO_DOWNFALL,
    "Downfall line of /tw biome info, only for a custom biome that sets one. {value} is the downfall.",
)
private val infoBaseBiomeText by snippet(
    INFO + "base_biome", BiomeTextDefaults.INFO_BASE_BIOME,
    "Base biome line of /tw biome info, only for a custom biome that has one. {value} is the base biome key.",
)

private val helpTitleText by snippet(
    HELP + "title", BiomeTextDefaults.HELP_TITLE,
    "Title of the command list shown by /tw biome. The help is one snippet per line so that each line can be reworded on its own.",
)
private val helpListText by snippet(HELP + "list", BiomeTextDefaults.HELP_LIST, "Help line for /tw biome list.")
private val helpInfoText by snippet(HELP + "info", BiomeTextDefaults.HELP_INFO, "Help line for /tw biome info.")
private val helpApplyText by snippet(HELP + "apply", BiomeTextDefaults.HELP_APPLY, "Help line for /tw biome apply.")
private val helpRefreshText by snippet(HELP + "refresh", BiomeTextDefaults.HELP_REFRESH, "Help line for /tw biome refresh.")
private val helpRegionText by snippet(HELP + "region", BiomeTextDefaults.HELP_REGION, "Help line for /tw biome region.")

private val regionNoSelectionText by snippet(
    REGION + "no_selection", BiomeTextDefaults.REGION_NO_SELECTION,
    "Reply of /tw biome region when the sender has no WorldEdit selection or WorldEdit is not installed.",
)
private val regionPageNotFoundText by snippet(
    REGION + "page_not_found", BiomeTextDefaults.REGION_PAGE_NOT_FOUND,
    "Reply of /tw biome region when the page holding the entry cannot be found. {entry} is the entry name.",
)
private val regionWriteFailedText by snippet(
    REGION + "write_failed", BiomeTextDefaults.REGION_WRITE_FAILED,
    "Reply of /tw biome region when a corner could not be written. {failures} lists the causes, in English.",
)
private val regionDoneText by snippet(
    REGION + "done", BiomeTextDefaults.REGION_DONE,
    "Reply of /tw biome region once both corners are written. {entry} is the entry name, {min} and {max} the corners as \"x, y, z\", {world} the world name.",
)
private val regionPublishHintText by snippet(
    REGION + "publish_hint", BiomeTextDefaults.REGION_PUBLISH_HINT,
    "Second reply of /tw biome region: a reminder that the page must be published.",
)

private val refreshNoneText by snippet(
    REFRESH + "none", BiomeTextDefaults.REFRESH_NONE,
    "Reply of /tw biome refresh when no painted chunk is near the player. {player} is the player.",
)
private val refreshDoneText by snippet(
    REFRESH + "done", BiomeTextDefaults.REFRESH_DONE,
    "Reply of /tw biome refresh. {chunks} is the number of chunks resent, {player} the player who gets them.",
)

private val applyUnknownText by snippet(
    APPLY + "unknown", BiomeTextDefaults.APPLY_UNKNOWN,
    "Reply of /tw biome apply for a biome that does not exist. {biome} is what was typed.",
)
private val applyUnknownHintText by snippet(
    APPLY + "unknown_hint", BiomeTextDefaults.APPLY_UNKNOWN_HINT,
    "Second reply of /tw biome apply for an unknown biome.",
)
private val applyNothingText by snippet(
    APPLY + "nothing", BiomeTextDefaults.APPLY_NOTHING,
    "Reply of /tw biome apply when there is nothing to paint at the location.",
)
private val applyDoneText by snippet(
    APPLY + "done", BiomeTextDefaults.APPLY_DONE,
    "Reply of /tw biome apply. {biome} is the readable biome name, {cells} the cells written, {chunks} the chunks touched.",
)

private val unknownValueText by snippet(
    PLACEHOLDER + "unknown", BiomeTextDefaults.UNKNOWN_VALUE,
    "What the temperature and downfall placeholders and variables return for a biome that is not a custom one.",
)

/**
 * Declares every snippet right away. A snippet only reaches the snippets file once its declaration has
 * run, so without this call the texts would be missing from the file until a command first used them.
 */
internal fun registerBiomeTextSnippets() = Unit

/** The texts of the `/tw biome` commands and of the biome placeholders, filled in. */
internal object BiomeTexts {

    val listEmpty get() = listEmptyText
    val infoCustomYes get() = infoCustomYesText
    val infoCustomNo get() = infoCustomNoText
    val regionNoSelection get() = regionNoSelectionText
    val regionPublishHint get() = regionPublishHintText
    val applyUnknownHint get() = applyUnknownHintText
    val applyNothing get() = applyNothingText
    val unknownValue get() = unknownValueText

    fun listHeader(count: Int) = TextTemplate.fill(listHeaderText, mapOf("count" to "$count"))

    /** [unsupportedReason] is null when the server supports live injection. */
    fun listStrategy(unsupportedReason: String?): String {
        val strategy = if (unsupportedReason == null) {
            strategyLiveText
        } else {
            TextTemplate.fill(strategyUnsupportedText, mapOf("reason" to TextTemplate.safe(unsupportedReason)))
        }
        return TextTemplate.fill(listStrategyText, mapOf("strategy" to strategy))
    }

    fun listEntry(name: String, key: String, state: BiomeListState, temperature: Double?, downfall: Double?): String {
        val (icon, status) = when (state) {
            BiomeListState.LIVE -> iconLiveText to statusLiveText
            BiomeListState.WAITING_FOR_RESTART -> iconWaitingText to statusWaitingText
            BiomeListState.NOT_REGISTERED -> iconMissingText to statusMissingText
        }
        return TextTemplate.fill(
            listEntryText,
            mapOf(
                "icon" to icon,
                "status" to status,
                "name" to name,
                "key" to key,
                "temperature" to optionalValue(listTemperatureText, temperature),
                "downfall" to optionalValue(listDownfallText, downfall),
            ),
        )
    }

    fun infoHeader(player: String) = TextTemplate.fill(infoHeaderText, mapOf("player" to player))
    fun infoName(name: String) = TextTemplate.fill(infoNameText, mapOf("value" to name))
    fun infoId(key: String) = TextTemplate.fill(infoIdText, mapOf("value" to key))
    fun infoTemperature(value: Double) = TextTemplate.fill(infoTemperatureText, mapOf("value" to "$value"))
    fun infoDownfall(value: Double) = TextTemplate.fill(infoDownfallText, mapOf("value" to "$value"))
    fun infoBaseBiome(key: String) = TextTemplate.fill(infoBaseBiomeText, mapOf("value" to key))

    /** The command list: a blank line, the title, a blank line, one line per command, a blank line. */
    fun help(): String = listOf(helpListText, helpInfoText, helpApplyText, helpRefreshText, helpRegionText)
        .joinToString(separator = "\n", prefix = "\n$helpTitleText\n\n", postfix = "\n")

    fun regionPageNotFound(entry: String) =
        TextTemplate.fill(regionPageNotFoundText, mapOf("entry" to TextTemplate.safe(entry)))

    fun regionWriteFailed(failures: List<String>) =
        TextTemplate.fill(regionWriteFailedText, mapOf("failures" to TextTemplate.safe(failures.joinToString(", "))))

    fun regionDone(entry: String, min: String, max: String, world: String) = TextTemplate.fill(
        regionDoneText,
        mapOf("entry" to TextTemplate.safe(entry), "min" to min, "max" to max, "world" to TextTemplate.safe(world)),
    )

    fun refreshNone(player: String) = TextTemplate.fill(refreshNoneText, mapOf("player" to player))

    fun refreshDone(chunks: Int, player: String) =
        TextTemplate.fill(refreshDoneText, mapOf("chunks" to "$chunks", "player" to player))

    fun applyUnknown(biome: String) = TextTemplate.fill(applyUnknownText, mapOf("biome" to TextTemplate.safe(biome)))

    fun applyDone(biome: String, cells: Int, chunks: Int) =
        TextTemplate.fill(applyDoneText, mapOf("biome" to biome, "cells" to "$cells", "chunks" to "$chunks"))

    private fun optionalValue(template: String, value: Double?): String =
        if (value == null) "" else TextTemplate.fill(template, mapOf("value" to "$value"))
}

/** Where a custom biome stands in the registry, as `/tw biome list` words it. */
internal enum class BiomeListState { LIVE, WAITING_FOR_RESTART, NOT_REGISTERED }
