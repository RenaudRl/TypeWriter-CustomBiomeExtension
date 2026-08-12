package btcrenaud.custombiome.service

import org.bukkit.block.Biome
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which biome each player was last seen in.
 *
 * This state used to live in a companion object on the event entry, which broke two rules at once:
 * entries must stay stateless, and both the enter and leave listeners mutated it independently.
 * Because enter overwrote the previous biome before leave read it, `leave_biome_event` only fired
 * when the listeners happened to run in the right order. Here the transition is computed once and
 * both events are derived from the same snapshot.
 */
object BiomeTrackingService {

    private val lastBiomes = ConcurrentHashMap<UUID, Biome>()

    /** A player moving from one biome to another. */
    data class Transition(
        val player: Player,
        val from: Biome?,
        val to: Biome,
    )

    /** Memo of the last computed transition, so both listeners of a move agree on it. */
    private class Memo(val biome: Biome, val transition: Transition?) {
        var remainingReads: Int = 1
    }

    private val memos = ConcurrentHashMap<UUID, Memo>()

    /**
     * Records [current] for [player] and returns the transition when the biome actually changed.
     *
     * The enter and leave listeners both call this for the same move. The first call computes the
     * transition, the second replays it, so leave no longer depends on running before enter.
     */
    @Synchronized
    fun observe(player: Player, current: Biome): Transition? {
        val memo = memos[player.uniqueId]
        if (memo != null && memo.biome == current && memo.remainingReads > 0) {
            memo.remainingReads--
            return memo.transition
        }

        val previous = lastBiomes.put(player.uniqueId, current)
        val transition = if (previous == current) null else Transition(player, previous, current)
        memos[player.uniqueId] = Memo(current, transition)

        if (transition != null) BiomeDiscoveryService.discover(player, current.key)

        return transition
    }

    /** Seeds the tracker without emitting a transition — used on join and on extension start. */
    fun prime(player: Player, biome: Biome) {
        lastBiomes[player.uniqueId] = biome
    }

    fun forget(playerId: UUID) {
        lastBiomes.remove(playerId)
        memos.remove(playerId)
    }

    fun clear() {
        lastBiomes.clear()
        memos.clear()
    }
}
