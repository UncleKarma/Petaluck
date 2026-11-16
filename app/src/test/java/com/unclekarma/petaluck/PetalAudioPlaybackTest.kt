package com.unclekarma.petaluck

import com.unclekarma.petaluck.scene.OneShotPlayback
import org.junit.Assert.*
import org.junit.Test

class PetalAudioPlaybackTest {
    private class Output {
        var running = false
        var starts = 0
        var stops = 0
        var failStart = false
        val player = OneShotPlayback(3600, startOutput = {
            running = true
            starts++
            if (failStart) throw IllegalStateException("Native play failed")
        }, stopOutput = { running = false; stops++ })
    }

    @Test fun finalSampleStopsTheOutputWithoutWaitingForAnotherFlower() {
        val out = Output()
        val cue = out.player.play()
        assertFalse(out.player.completed(cue, 3599))
        assertTrue(out.running)
        assertTrue(out.player.completed(cue, 3600))
        assertFalse(out.running)
        assertEquals(1, out.stops)
        assertFalse(out.player.completed(cue, 3600))
        assertFalse(out.player.timeout(cue))
        assertEquals(1, out.stops)
    }

    @Test fun missingCompletionStillStopsAndAnOldTimeoutCannotCutOffANewPluck() {
        val out = Output()
        val old = out.player.play()
        val next = out.player.play()
        assertFalse(out.player.timeout(old))
        assertTrue(out.running)
        assertTrue(out.player.timeout(next))
        assertFalse(out.running)
        assertEquals(2, out.stops)
    }

    @Test fun delayedMarkerCannotStopAFreshlyRewoundCue() {
        val out = Output()
        val old = out.player.play()
        val next = out.player.play()
        assertFalse(out.player.completed(old, 3600))
        // Android may dispatch an old queued marker to the current listener; check the head too.
        assertFalse(out.player.completed(next, 0))
        assertTrue(out.running)
        assertTrue(out.player.completed(next, 3600))
        assertFalse(out.running)
    }

    @Test fun pauseOrTeardownStopsImmediatelyAndInvalidatesPendingCallbacks() {
        val out = Output()
        val cue = out.player.play()
        out.player.stop()
        out.player.stop()
        assertFalse(out.running)
        assertFalse(out.player.completed(cue, 3600))
        assertFalse(out.player.timeout(cue))
        assertEquals(1, out.stops)
        // Resume does not play anything until another successful tear requests a new cue.
        assertEquals(1, out.starts)
    }

    @Test fun failedNativeStartStopsAnyPartialPlaybackAndAllowsTheNextCue() {
        val out = Output().apply { failStart = true }
        assertThrows(IllegalStateException::class.java) { out.player.play() }
        assertFalse(out.running)
        assertEquals(1, out.stops)
        out.failStart = false
        val cue = out.player.play()
        assertTrue(out.running)
        assertTrue(out.player.completed(cue, 3600))
        assertEquals(2, out.stops)
    }

    @Test fun repeatedPlucksAlwaysReturnToStoppedBetweenCues() {
        val out = Output()
        repeat(600) { index ->
            val cue = out.player.play()
            assertTrue(out.running)
            if (index % 3 == 0) out.player.timeout(cue)
            else out.player.completed(cue, 3600)
            assertFalse("Cue $index left an active output", out.running)
        }
        assertEquals(600, out.starts)
        assertEquals(600, out.stops)
    }
}
