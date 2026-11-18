package com.unclekarma.petaluck

import com.unclekarma.petaluck.domain.CompletionBufferGate
import org.junit.Assert.*
import org.junit.Test

class CompletionBufferGateTest {
    @Test fun threeRoundsEraseThenDisableTheHitSurfaceAndRequireFreshReveal() {
        val gate = CompletionBufferGate()
        for (round in 1L..3L) {
            assertFalse("No completion hit surface over a playable flower", gate.enabled)
            gate.update(round, true)
            assertTrue(gate.enabled)
            assertNull(gate.presentedRound)
            val reveal = checkNotNull(gate.request)
            assertEquals(round, reveal.revealRound)
            assertEquals(CompletionBufferGate.Draw.Blank, reveal.draw)
            assertTrue(gate.committed(reveal))
            assertNull(gate.presentedRound)
            assertFalse(gate.advanceFade(.05f))
            val blessing = checkNotNull(gate.request)
            assertEquals(CompletionBufferGate.Draw.Blessing, blessing.draw)
            assertFalse(gate.committed(reveal))
            assertTrue(gate.committed(blessing))
            assertEquals(0f, gate.fadeProgress, 0f)
            assertEquals(round, gate.presentedRound)

            repeat(12) { frame ->
                gate.advanceFade(.05f)
                assertEquals((frame + 1) / 12f, gate.fadeProgress, .00001f)
            }
            gate.update(0, false) // Replay publishes Loading before the next round exists.
            assertEquals(0f, gate.fadeProgress, 0f)
            assertFalse(gate.committed(blessing))
            assertTrue("Must still draw the erase before disabling", gate.enabled)
            assertNull(gate.presentedRound)
            val erase = checkNotNull(gate.request)
            assertNull(erase.revealRound)
            gate.update(round + 1, false)
            assertEquals("Loading ID changes must not invalidate the erase", erase, gate.request)
            assertTrue(gate.committed(erase))
            assertFalse("Blank commit must disable the outgoing hit surface", gate.enabled)
            assertNull(gate.request)
            assertFalse(gate.committed(reveal))
            assertFalse("Stale reveal must not restore the hit surface", gate.enabled)
        }
    }

    @Test fun completingToCompletedDoesNotRestartTheFade() {
        val gate = CompletionBufferGate()
        gate.update(1, true)
        val reveal = checkNotNull(gate.request)
        gate.update(1, true)
        assertEquals(reveal, gate.request)
        gate.committed(reveal)
        val blessing = checkNotNull(gate.request)
        gate.update(1, true)
        assertTrue(gate.committed(blessing))
        gate.advanceFade(.1f)
        gate.update(1, true)
        assertEquals(1f / 6f, gate.fadeProgress, .00001f)
        assertEquals(1L, gate.presentedRound)
        assertNull(gate.request)
    }

    @Test fun lateEraseCannotHideTheNextCompletion() {
        val gate = CompletionBufferGate()
        gate.update(1, true)
        gate.committed(checkNotNull(gate.request))
        val outgoingBlessing = checkNotNull(gate.request)
        gate.update(0, false)
        val erase = checkNotNull(gate.request)
        gate.update(2, true)
        assertFalse(gate.committed(outgoingBlessing))
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
        assertFalse(gate.enabled)
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
