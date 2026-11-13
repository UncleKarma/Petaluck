package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.domain.*
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI

class HeldPetalRotationTest {
    private val x = Vector3(1f, 0f, 0f)
    private val y = Vector3(0f, 1f, 0f)
    private val z = Vector3(0f, 0f, 1f)
    private fun turn(axis: Vector3, degrees: Float) = Quat(axis, degrees * PI.toFloat() / 180f)
    private fun assertVector(expected: Vector3, actual: Vector3) {
        assertEquals(expected.x, actual.x, .00001f)
        assertEquals(expected.y, actual.y, .00001f)
        assertEquals(expected.z, actual.z, .00001f)
    }
    private fun assertRotation(expected: Quat, actual: Quat?) {
        assertNotNull(actual)
        for (axis in listOf(x, y, z)) assertVector(expected.rotateVector(axis), actual!!.rotateVector(axis))
    }

    @Test fun detachUsesTheCrossingPoseWithoutApplyingEarlierHandRotation() {
        val motion = HeldPetalRotation()
        motion.observe(turn(x, 15f))
        assertNull(motion.rotation)
        val handAtTear = turn(y, 123f)
        val loadedPetal = turn(z, -31f) * turn(x, 47f)
        motion.observe(handAtTear)
        motion.detach(loadedPetal)
        assertRotation(loadedPetal, motion.rotation)
        repeat(20) {
            motion.observe(handAtTear)
            assertRotation(loadedPetal, motion.rotation)
        }
    }

    @Test fun handDeltaActsInSceneAxesAndRetainsThePetalsInitialOrientation() {
        val motion = HeldPetalRotation()
        val handAtTear = turn(x, 90f)
        motion.observe(handAtTear)
        motion.detach(turn(y, 90f))
        motion.observe(turn(z, 90f) * handAtTear)
        // 花瓣的 Z 原来指向 X；场景 Z 轴再旋转90度后应指向 Y。
        assertVector(y, motion.rotation!!.rotateVector(z))
        assertVector(Vector3(0f, 0f, -1f), motion.rotation!!.rotateVector(x))
    }

    @Test fun allThreeAxesFollowInBothDirectionsWithoutAccumulatedDrift() {
        for (axis in listOf(x, y, z)) {
            val motion = HeldPetalRotation()
            motion.observe(Quat.identity())
            motion.detach(Quat.identity())
            for (degrees in listOf(90f, -90f, 179f, 181f, 270f, 360f, 0f)) {
                repeat(100) { motion.observe(turn(axis, degrees)) }
                assertRotation(turn(axis, degrees), motion.rotation)
            }
        }
    }

    @Test fun equivalentQuaternionSignsDoNotFlipThePetal() {
        val motion = HeldPetalRotation()
        val baseline = turn(y, 39f)
        motion.observe(baseline)
        motion.detach(turn(x, 27f))
        val hand = turn(z, 64f) * baseline
        motion.observe(hand)
        val before = motion.rotation
        motion.observe(Quat(-hand.x, -hand.y, -hand.z, -hand.w))
        assertRotation(before!!, motion.rotation)
    }

    @Test fun missingPoseAtDetachAnchorsOnTheFirstValidPoseWithoutJumping() {
        val motion = HeldPetalRotation()
        motion.observe(turn(z, 12f))
        motion.observe(null)
        motion.detach(Quat.identity())
        motion.observe(null)
        motion.observe(turn(x, 90f))
        assertRotation(Quat.identity(), motion.rotation)
        motion.observe(turn(x, 180f))
        assertVector(z, motion.rotation!!.rotateVector(y))
    }

    @Test fun missingPoseWhileHeldPreservesRotationAndDoesNotDisablePositionDragging() {
        val motion = HeldPetalRotation()
        val drag = PetalDragMotion()
        drag.move(Vec(.05f)); drag.detach(Vec(.08f))
        motion.observe(Quat.identity()); motion.detach(Quat.identity())
        motion.observe(turn(z, 90f))
        motion.observe(null)
        drag.move(Vec(.25f, .12f, -.04f))
        assertEquals(Vec(.28f, .12f, -.04f), drag.detachedPosition)
        assertVector(y, motion.rotation!!.rotateVector(x))
        motion.observe(turn(z, 180f))
        assertVector(Vector3(-1f, 0f, 0f), motion.rotation!!.rotateVector(x))
    }

    @Test fun releaseSnapshotsKeepFinalPositionAndRotationForBothDragPathsAfterClear() {
        for (remote in listOf(false, true)) {
            val rotation = HeldPetalRotation()
            val direct = PetalDragMotion()
            val gaze = RemotePetalDrag()
            val tear = Vec(.08f, .02f, -.01f)
            rotation.observe(Quat.identity())
            if (remote) {
                gaze.start(); gaze.moveBy(Vec(.04f))
                gaze.advance { _, crossing ->
                    assertTrue(crossing)
                    rotation.detach(Quat.identity())
                    tear
                }
                gaze.moveBy(Vec(.16f, .1f, -.2f))
            } else {
                direct.move(Vec(.04f)); direct.detach(tear)
                rotation.detach(Quat.identity())
                direct.move(Vec(.2f, .1f, -.2f))
            }
            rotation.observe(turn(z, 90f))
            val fallPosition = if (remote) gaze.advance { _, _ -> error("Already detached") } else direct.detachedPosition
            val fallRotation = rotation.rotation
            direct.clear(); gaze.clear(); rotation.clear()
            rotation.observe(turn(x, 90f))
            assertNull(rotation.rotation)
            val p = checkNotNull(fallPosition)
            assertVector(Vector3(.24f, .12f, -.21f), Vector3(p.x, p.y, p.z))
            assertVector(y, fallRotation!!.rotateVector(x))
            rotation.detach(turn(y, 17f))
            assertRotation(turn(y, 17f), rotation.rotation)
        }
    }

    @Test fun onlyCapturedPointerCanDriveRotationEvenAfterKindChangesOrLeavingHit() {
        for (kind in listOf(PinchDragTracker.Kind.Direct, PinchDragTracker.Kind.Remote)) {
            val tracker = PinchDragTracker()
            val rotation = HeldPetalRotation()
            fun sample(hand: Long, down: Boolean, currentKind: PinchDragTracker.Kind, q: Quat) {
                val step = tracker.update(
                    PinchDragTracker.Sample(hand, down, false, true, currentKind, if (down) 0 else null),
                    position = { Vec(1f) }, grab = { _, _ -> true })
                if (step is PinchDragTracker.Step.Moved) rotation.observe(q)
            }
            sample(1, true, kind, Quat.identity())
            sample(1, false, kind, Quat.identity()) // Remote first move only anchors position.
            rotation.observe(Quat.identity()); rotation.detach(Quat.identity())
            sample(2, true, kind, turn(x, 90f))
            sample(2, false, kind, turn(x, 90f))
            assertRotation(Quat.identity(), rotation.rotation)
            sample(1, false, PinchDragTracker.Kind.Other, turn(z, 90f))
            assertVector(y, rotation.rotation!!.rotateVector(x))
        }
    }
}
