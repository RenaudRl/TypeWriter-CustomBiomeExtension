package btcrenaud.custombiome.text

/**
 * What every text of the extension says until an admin changes its snippet.
 *
 * Kept apart from the snippets themselves (see `BiomeTexts.kt`) because declaring a snippet needs the
 * running engine, while a default is plain text a test can read. `{name}` is a placeholder filled in
 * by [TextTemplate.fill].
 */
internal object BiomeTextDefaults {

    private const val GRADIENT_OPEN = "<gradient:#00d4ff:#0099ff><b>"
    private const val GRADIENT_CLOSE = "</b></gradient>"

    const val LIST_EMPTY = "<yellow>No custom biomes registered.</yellow>"
    const val LIST_HEADER = "\n${GRADIENT_OPEN}Custom Biomes ({count})$GRADIENT_CLOSE"
    const val LIST_STRATEGY = "<dark_gray>Strategy: {strategy}</dark_gray>\n"
    const val STRATEGY_LIVE = "live registry injection (reflection)"
    const val STRATEGY_UNSUPPORTED = "unsupported ({reason})"
    const val LIST_ICON_LIVE = "<#7ed957>•</#7ed957>"
    const val LIST_ICON_WAITING = "<#ffcc00>⚠</#ffcc00>"
    const val LIST_ICON_MISSING = "<#ff6b6b>✖</#ff6b6b>"
    const val LIST_STATUS_LIVE = "<green>Live in the registry</green>"
    const val LIST_STATUS_WAITING = "<yellow>Written to the datapack — needs a restart</yellow>"
    const val LIST_STATUS_MISSING = "<red>Not registered</red>"
    const val LIST_ENTRY = "<hover:show_text:'{status}\n<gray>Click to copy: {key}</gray>'>" +
        "<click:copy_to_clipboard:'{key}'>{icon} <white>{name}</white> " +
        "<#a0a0a0>({key})</#a0a0a0>{temperature}{downfall}</click></hover>"
    const val LIST_TEMPERATURE = " <gray>T:{value}</gray>"
    const val LIST_DOWNFALL = " <gray>D:{value}</gray>"

    const val INFO_HEADER = "\n${GRADIENT_OPEN}Biome Info for {player}$GRADIENT_CLOSE\n"
    const val INFO_NAME = "<gray>Name:</gray> <white>{value}</white>"
    const val INFO_ID = "<gray>ID:</gray> <white>{value}</white>"
    const val INFO_CUSTOM_YES = "<gray>Custom:</gray> <green>Yes</green>"
    const val INFO_CUSTOM_NO = "<gray>Custom:</gray> <gray>No</gray>"
    const val INFO_TEMPERATURE = "<gray>Temperature:</gray> <white>{value}</white>"
    const val INFO_DOWNFALL = "<gray>Downfall:</gray> <white>{value}</white>"
    const val INFO_BASE_BIOME = "<gray>Base Biome:</gray> <white>{value}</white>"

    const val HELP_TITLE = "${GRADIENT_OPEN}Custom Biome Commands$GRADIENT_CLOSE"
    const val HELP_LIST = "<white>/tw biome list</white> <gray>- List all custom biomes</gray>"
    const val HELP_INFO = "<white>/tw biome info [player]</white> <gray>- Show current biome info</gray>"
    const val HELP_APPLY = "<white>/tw biome apply <biome> [radius]</white> <gray>- Paint a biome around a player</gray>"
    const val HELP_REFRESH = "<white>/tw biome refresh [radius]</white> <gray>- Resend biome data</gray>"
    const val HELP_REGION =
        "<white>/tw biome region <entry></white> <gray>- Fill an entry's corners from your WorldEdit selection</gray>"

    const val REGION_NO_SELECTION = "<red>No WorldEdit selection (or WorldEdit is not installed).</red>"
    const val REGION_PAGE_NOT_FOUND = "<red>Could not find the page holding '{entry}'.</red>"
    const val REGION_WRITE_FAILED = "<red>Could not write the region: {failures}</red>"
    const val REGION_DONE = "Region of <white>{entry}</white> set to <green>{min}</green> → <green>{max}</green> in {world}."
    const val REGION_PUBLISH_HINT = "<gray>Publish the page for it to take effect.</gray>"

    const val REFRESH_NONE = "<yellow>No painted chunks near {player} to refresh.</yellow>"
    const val REFRESH_DONE = "Resent biome data for <green>{chunks}</green> painted chunk(s) to {player}."

    const val APPLY_UNKNOWN = "<red>Unknown biome: {biome}</red>"
    const val APPLY_UNKNOWN_HINT = "<gray>Use /tw biome list to see available custom biomes.</gray>"
    const val APPLY_NOTHING = "<yellow>Nothing to paint at that location.</yellow>"
    const val APPLY_DONE = "Painting <blue>{biome}</blue> over <green>{cells}</green> cell(s) across {chunks} chunk(s)."

    const val UNKNOWN_VALUE = "unknown"
}
