package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.domain.PullRules
import com.example.flowertears.p18ab52cf.domain.StemMotion
import com.example.flowertears.p18ab52cf.domain.Vec
import org.junit.Assert.*
import org.junit.Test

class StemMotionTest {
    @Test fun detachKeepsLoadedBendAndSwingsBackWithDecreasingAmplitude() {
        val motion = StemMotion()
        val delta = Vec(.05f, .01f, -.03f)
        val loaded = motion.pull(delta, PullRules.strength(PullRules.distance(delta)))
        motion.release(rebound = true)
        assertEquals(loaded, motion.advance(0f))

        val opposite = checkNotNull(motion.advance(.22f))
        val sameSide = checkNotNull(motion.advance(.22f))
        assertTrue(opposite.x < 0f && opposite.z > 0f)
        assertTrue(sameSide.x > 0f && sameSide.z < 0f)
        assertTrue(sameSide.length() < opposite.length())
        assertTrue(opposite.length() < loaded.length() * .17f)
        assertTrue(sameSide.length() < loaded.length() * .03f)
        assertEquals(Vec(), motion.advance(.21f))
        assertNull(motion.advance(.01f))
    }

    @Test fun regrabKeepsCurrentPoseAndStopsThePreviousRebound() {
        val motion = StemMotion()
        motion.pull(Vec(.04f), 1f)
        motion.release(rebound = true)
        val current = checkNotNull(motion.advance(.22f))

        motion.grab()
        assertNull(motion.advance(.016f))
        assertEquals(0f, (current - motion.pull(Vec(.005f), 0f)).length(), .000001f)
        assertEquals(Vec(0f, 0f, 1f), motion.pull(Vec(0f, 0f, .04f), 1f))
    }

    @Test fun cancelledPullReturnsOnceAndResetClearsPendingMotion() {
        val motion = StemMotion()
        assertEquals(Vec(), motion.pull(Vec(0f, .04f, 0f), 1f))
        val loaded = motion.pull(Vec(.025f), .5f)
        motion.release(rebound = false)
        assertEquals(loaded * .5f, motion.advance(StemMotion.CANCEL_TIME * .5f))
        assertEquals(Vec(), motion.advance(StemMotion.CANCEL_TIME * .5f))
        assertNull(motion.advance(.01f))

        motion.pull(Vec(.04f), 1f)
        motion.release(rebound = true)
        motion.advance(.1f)
        assertEquals(Vec(), motion.reset())
        assertNull(motion.advance(.01f))
        motion.grab()
        assertEquals(Vec(), motion.pull(Vec(), 0f))
    }
}
