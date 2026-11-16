package com.unclekarma.petaluck.domain

/** Main-thread, in-memory debug selection. Never persisted or consumed by sampling. */
class DebugRoseColor {
    var selectedColor: Int = -1
        private set

    fun set(color: Int): Boolean {
        if (color !in -1 until RoseLayout.COLOR_COUNT) return false
        selectedColor = color
        return true
    }

    fun reset() { selectedColor = -1 }

    fun sample(seed: Long, history: RecentRoseColors, retry: RoundConfig? = null): RoundConfig {
        if (retry != null) return retry
        return if (selectedColor == -1) history.sample(seed) else
            RoseLayout.sample(seed, excludedColors = (0 until RoseLayout.COLOR_COUNT).filter { it != selectedColor }.toSet())
    }
}
