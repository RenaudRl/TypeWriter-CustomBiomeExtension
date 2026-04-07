package btc.renaud.custombiome.util

import java.util.Locale

/**
 * Utility for parsing and validating hex colors.
 */
object ColorUtils {
    
    /**
     * Parse a hex color string to an integer.
     * Supports formats: #RRGGBB, RRGGBB, 0xRRGGBB
     * 
     * @return RGB integer or null if invalid
     */
    fun parseHexColor(input: String?): Int? {
        if (input.isNullOrBlank()) return null
        
        val cleaned = input.trim()
            .removePrefix("#")
            .removePrefix("0x")
            .removePrefix("0X")
            .lowercase(Locale.ENGLISH)
        
        if (cleaned.length != 6) return null
        if (!cleaned.all { it in '0'..'9' || it in 'a'..'f' }) return null
        
        return runCatching { cleaned.toLong(16).toInt() }.getOrNull()
    }
    
    /**
     * Convert an integer color to hex string format.
     */
    fun toHexString(color: Int): String {
        return "#${color.toString(16).padStart(6, '0').uppercase()}"
    }
    
    /**
     * Validate if a string is a valid hex color.
     */
    fun isValidHexColor(input: String?): Boolean {
        return parseHexColor(input) != null
    }
}
