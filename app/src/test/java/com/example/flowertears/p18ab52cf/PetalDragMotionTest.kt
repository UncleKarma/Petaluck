package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.domain.*
import org.junit.Assert.*
import org.junit.Test

class PetalDragMotionTest {
    private fun assertVec(expected:Vec,actual:Vec?) {
        assertNotNull(actual)
        assertEquals(expected.x,actual!!.x,.000001f)
        assertEquals(expected.y,actual.y,.000001f)
        assertEquals(expected.z,actual.z,.000001f)
    }

    @Test fun followsPastTwelveCentimetersAndBackTowardsTheRootWhileStillHeld() {
        val motion=PetalDragMotion()
        val tearDelta=Vec(.041f)
        val tearPosition=Vec(.06f,.035f,-.012f)
        motion.move(tearDelta);motion.detach(tearPosition)
        for(delta in listOf(Vec(.25f),Vec(.31f,-.18f,.22f),Vec(-.2f,.15f,-.1f),Vec(.01f))) {
            motion.move(delta)
            assertVec(tearPosition+delta-tearDelta,motion.detachedPosition)
        }
    }

    @Test fun aLargeFirstSampleDoesNotJumpWhenDetachedOrWhenTheStemRebounds() {
        val motion=PetalDragMotion()
        val overshoot=Vec(.28f,.09f,-.16f)
        val loadedRootPose=Vec(.305f,.122f,-.14f)
        motion.move(overshoot);motion.detach(loadedRootPose)
        motion.move(overshoot)
        assertVec(loadedRootPose,motion.detachedPosition)
        // A held position depends only on gesture displacement, not the crown's return pose.
        motion.move(overshoot+Vec(0f,.1f,0f))
        assertVec(loadedRootPose+Vec(0f,.1f,0f),motion.detachedPosition)
    }

    @Test fun releaseSnapshotSurvivesClearingTheGestureAndLateMoves() {
        val motion=PetalDragMotion()
        motion.move(Vec(.05f));motion.detach(Vec(.07f,.03f))
        motion.move(Vec(.24f,.1f,-.08f))
        val fallStart=checkNotNull(motion.detachedPosition)
        motion.clear();motion.move(Vec(1f,1f,1f))
        assertNull(motion.detachedPosition)
        assertVec(Vec(.26f,.13f,-.08f),fallStart)
    }

    @Test fun nextGrabAndRoundStartWithoutThePreviousDetachedOffset() {
        val motion=PetalDragMotion()
        motion.move(Vec(.2f));motion.detach(Vec(.25f))
        motion.clear()
        assertEquals(Vec(),motion.delta);assertNull(motion.detachedPosition)
        motion.move(Vec(0f,.045f));motion.detach(Vec(-.02f,.08f,.01f))
        motion.move(Vec(.1f,.2f,.3f))
        assertVec(Vec(.08f,.235f,.31f),motion.detachedPosition)
    }
}
