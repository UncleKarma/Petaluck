package com.unclekarma.petaluck

import com.unclekarma.petaluck.domain.CompletionBufferGate
import org.junit.Assert.*
import org.junit.Test

class CompletionBufferGateTest {
    @Test fun threeRoundsKeepBlankSurfaceAliveAndRequireFreshReveal() {
        val gate = CompletionBufferGate()
        for (round in 1L..3L) {
            assertEquals(round > 1, gate.enabled)
            gate.update(round, true)
            assertTrue(gate.enabled)
            assertNull(gate.presentedRound)
            val reveal = checkNotNull(gate.request)
            assertEquals(round, reveal.revealRound)
            assertTrue(gate.committed(reveal))
            assertEquals(round, gate.presentedRound)

            gate.update(0, false) // Replay publishes Loading before the next round exists.
            assertTrue("Must still draw the erase before disabling", gate.enabled)
            assertNull(gate.presentedRound)
            val erase = checkNotNull(gate.request)
            assertNull(erase.revealRound)
            gate.update(round + 1, false)
            assertEquals("Loading ID changes must not invalidate the erase", erase, gate.request)
            assertTrue(gate.committed(erase))
            assertTrue(gate.enabled)
            assertNull(gate.request)
            assertFalse(gate.committed(reveal))
        }
    }

    @Test fun completingToCompletedDoesNotRestartTheFade() {
        val gate = CompletionBufferGate()
        gate.update(1, true)
        val reveal = checkNotNull(gate.request)
        gate.update(1, true)
        assertEquals(reveal, gate.request)
        gate.committed(reveal)
        gate.update(1, true)
        assertEquals(1L, gate.presentedRound)
        assertNull(gate.request)
    }

    @Test fun lateEraseCannotHideTheNextCompletion() {
        val gate = CompletionBufferGate()
        gate.update(1, true)
        gate.committed(checkNotNull(gate.request))
        gate.update(0, false)
        val erase = checkNotNull(gate.request)
        gate.update(2, true)
        assertFalse(gate.committed(erase))
        assertTrue(gate.enabled)
        assertEquals(2L, gate.request?.revealRound)
    }

    @Test fun leavingBeforeFirstDrawRejectsTheQueuedReveal() {
        val gate = CompletionBufferGate()
        gate.update(1, true)
        val reveal = checkNotNull(gate.request)
        gate.update(0, false)
        assertFalse(gate.committed(reveal))
        assertNull(gate.presentedRound)
        assertTrue(gate.committed(checkNotNull(gate.request)))
        assertTrue(gate.enabled)
    }

    @Test fun ownerReplacementRejectsSameRoundOldCallback() {
        val gate = CompletionBufferGate()
        gate.update(1, true)
        val oldOwner = checkNotNull(gate.request)
        gate.reset()
        gate.update(1, true)
        assertFalse(gate.committed(oldOwner))
        assertNull(gate.presentedRound)
        assertTrue(gate.committed(checkNotNull(gate.request)))
    }

    @Test fun initialLoadingHasNoEnabledHitSurface() {
        val gate = CompletionBufferGate()
        gate.update(0, false)
        gate.update(1, false)
        assertFalse(gate.enabled)
        assertNull(gate.request)
    }
}
