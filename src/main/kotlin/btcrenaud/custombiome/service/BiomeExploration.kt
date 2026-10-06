package btcrenaud.custombiome.service

/** The pure rule behind `explore_biome_objective`, kept free of the server so it can be tested. */
object BiomeExploration {

    /**
     * Whether what the player has discovered satisfies the objective.
     *
     * [required] is the list of biomes the objective asks for. With [requireAll] every one must be
     * discovered, otherwise any one is enough. An objective that asks for nothing is never complete,
     * so a typo in every identifier cannot complete a quest by itself.
     */
    fun <K> isComplete(required: Collection<K>, hasDiscovered: (K) -> Boolean, requireAll: Boolean): Boolean {
        if (required.isEmpty()) return false
        return if (requireAll) required.all(hasDiscovered) else required.any(hasDiscovered)
    }
}
