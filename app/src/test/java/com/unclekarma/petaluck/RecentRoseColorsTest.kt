package com.unclekarma.petaluck

import com.unclekarma.petaluck.domain.RecentRoseColors
import com.unclekarma.petaluck.domain.RoseLayout
import org.junit.Assert.*
import org.junit.Test

class RecentRoseColorsTest {
    @Test fun firstDrawHasAllEightColorsAndKeepsTheOriginalSeededLayout() {
        val history = RecentRoseColors()
        val seen = mutableSetOf<Int>()
        repeat(128) {
            val seed = it.toLong()
            val sampled = history.sample(seed)
            assertEquals(RoseLayout.sample(seed), sampled)
            seen += sampled.colorId
        }
        assertEquals((0 until RoseLayout.COLOR_COUNT).toSet(), seen)
    }

    @Test fun replaysExcludeExactlyTheAvailableRecentColors() {
        val history = RecentRoseColors()
        for (played in listOf(7, 0, 3, 2)) {
            history.recordReady(played)
            val recent = when (played) {
                7 -> setOf(7)
                0 -> setOf(7, 0)
                3 -> setOf(7, 0, 3)
                else -> setOf(0, 3, 2)
            }
            val eligible = (0 until RoseLayout.COLOR_COUNT).toSet() - recent
            val draws = (0L..127L).map(history::sample)
            assertEquals(eligible, draws.map { it.colorId }.toSet())
            assertTrue(draws.all { it.count == RoseLayout.sample(it.seed).count })
        }
    }

    @Test fun onlyReadyRoundsAdvanceTheWindowAndRetryOfReadyRoundDoesNotEvictAColor() {
        val history = RecentRoseColors()
        val ready = mutableListOf<Int>()
        repeat(100) {
            val seed = it.toLong()
            val candidate = history.sample(seed)
            assertFalse(candidate.colorId in ready.takeLast(3))
            assertEquals(candidate, history.sample(seed)) // An assembly attempt has no effect until ready.
            history.recordReady(candidate.colorId)
            history.recordReady(candidate.colorId)
            ready += candidate.colorId
        }
    }
}
