package btcrenaud.custombiome.entries.audience

import com.typewritermc.core.entries.Entry
import com.typewritermc.core.utils.point.Position

/**
 * An entry whose region is defined by two captured corners.
 *
 * Exists so `/tw biome region` can fill any of them without knowing which one it is holding.
 */
interface BiomeRegionHolder : Entry {
    val cornerA: Position
    val cornerB: Position
}
