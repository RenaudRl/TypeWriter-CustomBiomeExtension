package btcrenaud.custombiome.text

import net.kyori.adventure.text.minimessage.MiniMessage

/** The `{name}` placeholders of the texts an admin can edit in the snippets file. */
internal object TextTemplate {

    /** `{name}` in [text] becomes the matching value; a placeholder without a value stays as written. */
    fun fill(text: String, values: Map<String, String>): String =
        values.entries.fold(text) { filled, (name, value) -> filled.replace("{$name}", value) }

    /** A value typed by a person or raised by an error, safe to put inside a MiniMessage text. */
    fun safe(value: String): String = MiniMessage.miniMessage().escapeTags(value)
}
