package com.unclekarma.petaluck

import com.unclekarma.petaluck.domain.RecentRoseColors
import com.unclekarma.petaluck.domain.RoseLayout
import org.junit.Assert.*
import org.junit.Test

class RecentRoseColorsTest {
    @Test fun normalSessionVisitsEachSolidColorOnceThenColorful() {
        for (start in 0L..80L) {
            val history = RecentRoseColors()
            val seen = mutableSetOf<Int>()
            repeat(RoseLayout.BASE_COLOR_COUNT) { index ->
                val seed = start * 100 + index
                val round = history.sample(seed)
                assertTrue(round.colorId in 0 until RoseLayout.BASE_COLOR_COUNT)
                assertTrue(seen.add(round.colorId))
                assertEquals(round, history.sample(seed))
                history.recordReady(round.colorId)
                history.recordReady(round.colorId)
            }
            assertEquals((0 until RoseLayout.BASE_COLOR_COUNT).toSet(), seen)
            assertEquals(RoseLayout.MULTICOLOR, history.sample(start + 10000).colorId)
        }
    }

    @Test fun onlyReadySolidRoundsCountAndDebugColorfulDoesNotUnlock() {
        val history = RecentRoseColors()
        val first = history.sample(12)
        assertEquals(first, history.sample(12))
        history.recordReady(RoseLayout.MULTICOLOR)
        assertEquals(first, history.sample(12))
        history.recordReady(first.colorId)
        assertNotEquals(first.colorId, history.sample(12).colorId)
    }
}
