package com.unclekarma.petaluck

import com.unclekarma.petaluck.scene.EndingMusicPlayback
import com.unclekarma.petaluck.scene.PetalRainSchedule
import org.junit.Assert.*
import org.junit.Test

class PetalRainScheduleTest {
    private class Clock {
        data class Pending(val at: Long, val action: () -> Unit)
        var now = 0L
        val pending = mutableListOf<Pending>()
        val events = mutableListOf<String>()
        val rain = PetalRainSchedule(
            { delay, action -> pending += Pending(now + delay, action) },
            { pending.clear() },
            { events += "drain:$it" },
            { events += "finish:$it" },
        )
        fun advance(to: Long) {
            while (true) {
                val next = pending.minByOrNull { it.at } ?: break
                if (next.at > to) break
                pending.remove(next)
                now = next.at
                next.action()
            }
            now = to
        }
    }

    @Test fun lastPetalsFinishTheirSampledLifetimeAfterEmissionStops() {
        val c = Clock()
        c.now = 1234
        c.rain.start(7, 6_731)
        c.advance(11233)
        assertTrue(c.events.isEmpty())
        c.advance(11234)
        assertEquals(listOf("drain:7"), c.events)
        c.advance(17964)
        assertEquals(listOf("drain:7"), c.events)
        c.advance(17965)
        assertEquals(listOf("drain:7", "finish:7"), c.events)
        assertTrue(c.pending.isEmpty())
    }

    @Test fun duplicateStartDoesNotExtendEitherDeadline() {
        val c = Clock()
        c.rain.start(1, 7_000)
        c.advance(9000)
        c.rain.start(1, 7_000)
        c.advance(10000)
        assertEquals(listOf("drain:1"), c.events)
        c.rain.start(1, 7_000)
        c.advance(17000)
        assertEquals(listOf("drain:1", "finish:1"), c.events)
    }

    @Test fun cancelledCallbacksCannotDrainOrDestroyNextRound() {
        val c = Clock()
        c.rain.start(1, 7_000)
        val old = c.pending.toList()
        c.advance(2000)
        c.rain.cancel()
        c.rain.start(2, 6_500)
        old.forEach { it.action() }
        assertTrue(c.events.isEmpty())
        c.advance(12000)
        assertEquals(listOf("drain:2"), c.events)
        old.forEach { it.action() }
        c.advance(18500)
        assertEquals(listOf("drain:2", "finish:2"), c.events)
    }

    @Test fun pauseDuringTailCancelsFinalDeadlineAndNeverResumes() {
        val c = Clock()
        c.rain.start(1, 7_000)
        c.advance(10000)
        val old = c.pending.toList()
        c.rain.cancel()
        c.rain.cancel()
        old.forEach { it.action() }
        c.advance(30000)
        assertEquals(listOf("drain:1"), c.events)
        assertTrue(c.pending.isEmpty())
    }

    @Test fun musicNaturalEndAtFourteenPointFiveDoesNotCutTheTail() {
        val c = Clock()
        val music = EndingMusicPlayback(
            {}, {}, {}, { c.rain.start(it, 7_000) }, { c.rain.cancel() },
            onFinished = {},
        )
        music.blessing(1, 0, 14500)
        c.advance(10000)
        c.advance(14500)
        music.tick(c.now, true)
        assertFalse(music.active)
        assertEquals(listOf("drain:1"), c.events)
        c.advance(16999)
        assertEquals(listOf("drain:1"), c.events)
        c.advance(17000)
        assertEquals(listOf("drain:1", "finish:1"), c.events)
    }
}
