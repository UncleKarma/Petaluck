package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.domain.*
import org.junit.Assert.*
import org.junit.Test

class DebugRoseColorTest {
    @Test fun startupAndResetUseNormalHistoryAwareSampling() {
        val selection = DebugRoseColor()
        val history = RecentRoseColors().apply { recordReady(0); recordReady(8); recordReady(2) }
        assertEquals(-1, selection.selectedColor)
        repeat(128) { assertEquals(history.sample(it.toLong()), selection.sample(it.toLong(), history)) }
        selection.set(8)
        selection.reset()
        assertEquals(-1, selection.selectedColor)
        assertEquals(history.sample(99), selection.sample(99, history))
    }
    @Test fun allColorsStaySelectedAcrossRoundsAndBypassRecentExclusions() {
        val selection = DebugRoseColor()
        val history = RecentRoseColors()
        for (color in 0 until RoseLayout.COLOR_COUNT) {
            assertTrue(selection.set(color))
            history.recordReady(color)
            repeat(32) {
                val round = selection.sample(it.toLong(), history)
                assertEquals(color, round.colorId)
                assertEquals(color, selection.selectedColor)
                assertEquals(RoseLayout.sample(it.toLong()).count, round.count)
                round.validate()
            }
        }
        assertTrue(selection.set(-1))
        repeat(32) { assertEquals(history.sample(it.toLong()), selection.sample(it.toLong(), history)) }
    }
    @Test fun badValuesAndErrorsDoNotChangeExistingRoundOrSelection() {
        val selection = DebugRoseColor().apply { set(8) }
        for (invalid in listOf(-2, RoseLayout.COLOR_COUNT, Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertFalse(selection.set(invalid))
            assertEquals(8, selection.selectedColor)
        }
        val history = RecentRoseColors()
        val round = selection.sample(123, history)
        selection.set(2)
        assertSame(round, selection.sample(456, history, retry = round))
        assertEquals(2, selection.sample(456, history).colorId)
        assertEquals(8, round.colorId)
    }
}
