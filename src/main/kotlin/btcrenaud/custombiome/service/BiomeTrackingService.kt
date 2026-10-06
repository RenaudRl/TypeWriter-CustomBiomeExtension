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
 * every listener of the same move is handed that same answer.
 */
object BiomeTrackingService {

    private val lastBiomes = ConcurrentHashMap<UUID, Biome>()

    /** A player moving from one biome to another. */
    data class Transition(
        val player: Player,
        val from: Biome?,
        val to: Biome,
    )

    /**
     * The answer given for one move, keyed by the move itself.
     *
     * Identity of the move, not a count of readers: the enter, leave and exploration listeners each
     * exist only while an entry of their kind does, so the number of readers varies, and a count
     * either starves the last one or replays a stale transition on the next move.
     */
    private class Memo(val move: Any, val transition: Transition?)

    private val memos = ConcurrentHashMap<UUID, Memo>()

    /**
     * Records [current] for [player] and returns the transition when the biome actually changed.
     *
     * Every listener of the same [move] (the event instance they all received) gets the answer
     * computed by the first one, whatever the number or order of listeners. Computation is locked
     * per player only: the move event runs on a different region thread for each player on Folia.
     */
    fun observe(player: Player, current: Biome, move: Any): Transition? {
        var result: Transition? = null
        memos.compute(player.uniqueId) { _, memo ->
            if (memo != null && memo.move === move) {
                result = memo.transition
                return@compute memo
            }

            val previous = lastBiomes.put(player.uniqueId, current)
            val transition = if (previous == current) null else Transition(player, previous, current)
            if (transition != null) BiomeDiscoveryService.discover(player, current.key)

            result = transition
            Memo(move, transition)
        }
        return result
    }

    /** Seeds the tracker without emitting a transition - used on join and on extension start. */
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
