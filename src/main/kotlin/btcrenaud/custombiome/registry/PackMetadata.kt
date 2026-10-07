package btcrenaud.custombiome.registry

import com.google.gson.JsonObject

/**
 * The `pack.mcmeta` of the generated datapack, in the form the running server's metadata codec accepts.
 *
 * Since data pack formats gained a minor number, the server refuses a bare `pack_format` for any
 * format above the legacy limit ("missing mandatory fields min_format and max_format"). It then logs
 * a parse error at every start and falls back to a lenient codec. Formats at or below the limit
 * predate `min_format`/`max_format` and only understand `pack_format`.
 */
internal object PackMetadata {

    /**
     * Last data pack format that predates `min_format`/`max_format`, used only when the server cannot
     * be asked. It is 81 on every server that has the new codec.
     */
    const val LEGACY_FORMAT_FALLBACK = 81

    /**
     * @param format major data pack format of the running server
     * @param legacyFormatLimit highest format that still takes a bare `pack_format`, read from the
     * server (`PackFormat.lastPreMinorVersion`)
     */
    fun json(description: String, format: Int, legacyFormatLimit: Int): JsonObject {
        val pack = JsonObject().apply { addProperty("description", description) }

        if (format <= legacyFormatLimit) {
            pack.addProperty("pack_format", format)
        } else {
            // A whole-number bound covers every minor of that major (min = N.0, max = N.*), so the
            // pack keeps loading across minor data pack bumps of the same major.
            pack.addProperty("min_format", format)
            pack.addProperty("max_format", format)
        }

        return JsonObject().apply { add("pack", pack) }
    }
}
