package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.domain.*
import org.junit.Assert.*
import org.junit.Test

class RemotePetalDragTest {
    private fun assertVec(expected: Vec, actual: Vec?) {
        assertNotNull(actual)
        assertEquals(expected.x, actual!!.x, .000001f)
        assertEquals(expected.y, actual.y, .000001f)
        assertEquals(expected.z, actual.z, .000001f)
    }

    @Test fun slowPullUsesNetDisplacementForDeadzoneRatherThanSampleLengthOrPathLength() {
        val drag = RemotePetalDrag().apply { start() }
        val strengths = mutableListOf<Float>()
        repeat(10) { drag.moveBy(Vec(.001f)) }
        assertNull(drag.advance { delta, crossing ->
            assertFalse(crossing)
            strengths += PullRules.strength(delta.length())
            null
        })
        assertEquals(0f, strengths[6], .000001f)
        assertTrue(strengths.last() > 0f)
        repeat(100) { drag.moveBy(Vec(.001f)); drag.moveBy(Vec(-.001f)) }
        drag.advance { _, crossing -> assertFalse(crossing); null }
        assertVec(Vec(.01f), drag.displacement)
    }

    @Test fun thresholdSampleSplitsAtFourCentimetersAndKeepsTheRemainderExactlyOnce() {
        val drag = RemotePetalDrag().apply { start() }
        val tearPose = Vec(.061f, .032f, -.013f)
        drag.moveBy(Vec(.005f))
        drag.advance { _, crossing -> assertFalse(crossing); null }
        drag.moveBy(Vec(.295f))
        var calls = 0
        val actual = drag.advance { delta, crossing ->
            calls++
            assertTrue(crossing)
            assertVec(Vec(.04f), delta)
            tearPose
        }
        assertEquals(1, calls)
        assertVec(tearPose + Vec(.26f), actual)
        repeat(30) { assertVec(actual!!, drag.advance { _, _ -> fail("Stationary frame pulled again"); null }) }
    }

    @Test fun exactThresholdDetachesWithoutExtraMotionInAllAxisDirections() {
        for (direction in listOf(Vec(1f), Vec(-1f), Vec(0f,1f), Vec(0f,-1f), Vec(0f,0f,1f), Vec(0f,0f,-1f))) {
            val drag = RemotePetalDrag().apply { start(); moveBy(direction * .04f) }
            val pose = Vec(-.04f, .08f, .03f)
            assertVec(pose, drag.advance { delta, crossing ->
                assertTrue(crossing)
                assertVec(direction * .04f, delta)
                pose
            })
        }
    }

    @Test fun obliqueCrossingFindsTheSphereExitRatherThanScalingTheWholeGesture() {
        val drag = RemotePetalDrag().apply { start(); moveBy(Vec(.024f)) }
        drag.advance { _, crossing -> assertFalse(crossing); null }
        drag.moveBy(Vec(0f, .06f))
        val pose = Vec(.07f, .08f, .09f)
        assertVec(pose + Vec(0f, .028f), drag.advance { delta, crossing ->
            assertTrue(crossing)
            assertVec(Vec(.024f, .032f), delta)
            pose
        })
    }

    @Test fun outwardThenBackWithinOneFrameStillDetachesOnce() {
        val drag = RemotePetalDrag().apply { start(); moveBy(Vec(.05f)); moveBy(Vec(-.05f)) }
        var tears = 0
        val pose = Vec(.08f, .02f, -.01f)
        assertVec(pose + Vec(-.04f), drag.advance { delta, crossing ->
            assertTrue(crossing)
            assertVec(Vec(.04f), delta)
            tears++
            pose
        })
        assertEquals(1, tears)
        drag.moveBy(Vec(-.3f, .2f, .1f))
        assertVec(pose + Vec(-.34f, .2f, .1f), drag.advance { _, _ -> fail("Detached petal pulled again"); null })
    }

    @Test fun reverseAcrossTheOriginUsesTheExitInTheNewDirection() {
        val drag = RemotePetalDrag().apply { start(); moveBy(Vec(.03f)) }
        drag.advance { _, crossing -> assertFalse(crossing); null }
        drag.moveBy(Vec(-.09f))
        assertVec(Vec(-.02f), drag.advance { delta, crossing ->
            assertTrue(crossing)
            assertVec(Vec(-.04f), delta)
            Vec()
        })
    }

    @Test fun releaseWithoutASceneFrameFlushesTheFinalPetalAfterCaptureIsCleared() {
        val session = RoseSession(1, RoseLayout.assemble(10, 17, 1)).apply { ready() }
        repeat(16) { id ->
            session.grab(1, id, 1); session.detach(1, id, 1); session.release(1, 1); session.removed(id)
        }
        val tracker = PinchDragTracker()
        val drag = RemotePetalDrag()
        val pose = Vec(.07f, .03f, -.01f)
        fun send(raw: Vec?, down: Boolean = false, up: Boolean = false) = tracker.update(
            PinchDragTracker.Sample(1, down, up, !up, PinchDragTracker.Kind.Remote, if (down) 16 else null),
            position = { check(!up); raw },
            grab = { id, hand -> session.grab(1, id, hand) },
            displacement = { _, previous, current -> (current - previous) * .001f })
        assertTrue(send(Vec(1000f), down = true) is PinchDragTracker.Step.Started)
        drag.start()
        for (raw in listOf(Vec(1050f), Vec(1080f, 20f))) {
            drag.moveBy((send(raw) as PinchDragTracker.Step.Moved).frameDelta)
        }
        assertEquals(16, session.detachedCount) // No scene frame has consumed the inputs.
        assertTrue(send(null, up = true) is PinchDragTracker.Step.Released)
        assertNull(tracker.positionSource)
        val fallStart = drag.advance { delta, crossing ->
            assertTrue(crossing)
            assertVec(Vec(.04f), delta)
            assertTrue(session.detach(1, 16, 1))
            assertEquals(Phase.FinalPetalHeld, session.phase)
            pose
        }
        assertVec(pose + Vec(.04f, .02f), fallStart)
        assertEquals(PetalState.Falling, session.release(1, 1))
        drag.clear()
        assertEquals(17, session.detachedCount)
        assertEquals(Phase.Completing, session.phase)
        session.removed(16)
        assertEquals(Phase.Completed, session.phase)
    }

    @Test fun clearingPendingAndHeldMotionPreventsLeakageIntoTheNextRound() {
        val drag = RemotePetalDrag().apply { start(); moveBy(Vec(.3f)) }
        drag.clear()
        drag.moveBy(Vec(1f))
        assertNull(drag.advance { _, _ -> fail("Inactive gesture consumed input"); null })
        drag.start()
        assertVec(Vec(), drag.displacement)
        drag.moveBy(Vec(.02f))
        assertNull(drag.advance { delta, crossing ->
            assertFalse(crossing); assertVec(Vec(.02f), delta); null
        })
        drag.moveBy(Vec(.03f))
        assertVec(Vec(.01f), drag.advance { _, _ -> Vec() })
        drag.clear(); drag.start()
        assertNull(drag.advance { _, _ -> fail("Previous held pose survived"); null })
    }

    @Test fun invalidSampleDoesNotPoisonTheGestureAndNoInputDoesNotApplyBend() {
        val drag = RemotePetalDrag().apply { start() }
        drag.moveBy(Vec(Float.NaN)); drag.moveBy(Vec(0f, Float.POSITIVE_INFINITY))
        assertNull(drag.advance { _, _ -> fail("Invalid input reached scene"); null })
        drag.moveBy(Vec(.02f))
        assertNull(drag.advance { delta, crossing ->
            assertFalse(crossing); assertVec(Vec(.02f), delta); null
        })
    }
}
