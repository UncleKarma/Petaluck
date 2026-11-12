package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.domain.*
import org.junit.Assert.*
import org.junit.Test

class IncrementalPetalDragTest {
    private fun assertVec(expected:Vec,actual:Vec?) {
        assertNotNull(actual)
        assertEquals(expected.x,actual!!.x,.000001f)
        assertEquals(expected.y,actual.y,.000001f)
        assertEquals(expected.z,actual.z,.000001f)
    }

    @Test fun detachFrameAndStationaryFramesKeepTheExactVisibleTearPose() {
        val motion=IncrementalPetalDrag()
        motion.moveBy(Vec(2f,3f,4f)) // Pre-tear movement must not enter detached motion.
        val visible=Vec(.064f,.032f,-.011f)
        motion.detach(visible)
        repeat(90) { assertVec(visible,motion.advance()) }
    }

    @Test fun eachSceneFrameAddsAllNewHandDeltasExactlyOnceInThreeDimensions() {
        val motion=IncrementalPetalDrag()
        val tear=Vec(.06f,.04f,-.01f)
        motion.detach(tear)
        motion.moveBy(Vec(.01f,-.02f,.03f))
        motion.moveBy(Vec(.02f,.01f,-.04f))
        val first=tear+Vec(.03f,-.01f,-.01f)
        assertVec(first,motion.advance())
        repeat(8) { assertVec(first,motion.advance()) } // No repeated movement on empty frames.
        val reverse=Vec(-.31f,.24f,.18f)
        motion.moveBy(reverse)
        assertVec(first+reverse,motion.advance()) // Full movement, no 12 cm cap.
    }

    @Test fun releaseFlushesPendingInputThenClearingPreventsAnyDelayedMoveOrNextRoundOffset() {
        val motion=IncrementalPetalDrag()
        motion.detach(Vec(.08f,.03f,-.02f))
        motion.moveBy(Vec(.12f,-.04f,.06f))
        // Production release drains pending movement before taking fallStart, even without a frame.
        val fallStart=checkNotNull(motion.advance())
        motion.clear();motion.moveBy(Vec(9f))
        assertNull(motion.advance())
        assertVec(Vec(.20f,-.01f,.04f),fallStart)
        motion.detach(Vec(-.02f,.07f,.01f))
        assertVec(Vec(-.02f,.07f,.01f),motion.advance())
    }

    @Test fun remoteUsesAdjacentSamplesInsteadOfReapplyingAReconvertedGestureTotal() {
        val tracker=PinchDragTracker()
        val motion=IncrementalPetalDrag()
        val origin=Vec(1800f,900f,1500f)
        var metersPerPixel=.001f
        fun send(raw:Vec,down:Boolean=false)=tracker.update(
            PinchDragTracker.Sample(1,down,false,true,PinchDragTracker.Kind.Remote,if(down)0 else null),
            position={raw},grab={_,_->true},displacement={_,previous,current->
                val d=current-previous
                Vec(d.x,-d.y,d.z)*metersPerPixel
            })
        send(origin,down=true)
        val atTear=origin+Vec(50f,0f,0f)
        val threshold=send(atTear) as PinchDragTracker.Step.Moved
        assertTrue(PullRules.distance(threshold.delta)>=PullRules.THRESHOLD)
        val visible=Vec(.07f,.03f,-.01f)
        motion.detach(visible) // Do not also add threshold.frameDelta: already represented in visible.
        metersPerPixel=.01f
        val stationary=send(atTear) as PinchDragTracker.Step.Moved
        assertEquals(.5f,stationary.delta.x,.000001f) // A total-based position would now jump.
        assertVec(Vec(),stationary.frameDelta)
        motion.moveBy(stationary.frameDelta)
        assertVec(visible,motion.advance())
        val next=send(atTear+Vec(2f,3f,-4f)) as PinchDragTracker.Step.Moved
        assertVec(Vec(.02f,-.03f,-.04f),next.frameDelta)
        motion.moveBy(next.frameDelta)
        assertVec(visible+Vec(.02f,-.03f,-.04f),motion.advance())
    }

    @Test fun unavailableRemoteSampleAndKindChangesKeepThePreviousValidHandSample() {
        val tracker=PinchDragTracker()
        val origin=Vec(1000f,2000f,1500f)
        var raw:Vec?=origin
        fun send(down:Boolean=false,kind:PinchDragTracker.Kind=PinchDragTracker.Kind.Other)=tracker.update(
            PinchDragTracker.Sample(1,down,false,true,kind,if(down)0 else null),
            position={source->assertEquals(PinchDragTracker.PositionSource.GlobalDevicePose,source);raw},
            grab={_,_->true},displacement={_,previous,current->(current-previous)*.001f})
        send(down=true,kind=PinchDragTracker.Kind.Remote)
        raw=origin+Vec(50f)
        send() as PinchDragTracker.Step.Moved
        raw=null
        assertTrue(send() is PinchDragTracker.Step.Unavailable)
        raw=Vec(Float.NaN)
        assertTrue(send() is PinchDragTracker.Step.Unavailable)
        raw=origin+Vec(54f,2f,-3f)
        val recovered=send() as PinchDragTracker.Step.Moved
        assertVec(Vec(.004f,.002f,-.003f),recovered.frameDelta)
        val unchanged=send() as PinchDragTracker.Step.Moved
        assertVec(Vec(),unchanged.frameDelta)
    }

    @Test fun finalRemotePetalFlushesBeforePoseFreeReleaseAndKeepsTheCompletionOrder() {
        val session=RoseSession(1,RoseLayout.assemble(10,17,1)).apply {ready()}
        repeat(16) { id->session.grab(1,id,1);session.detach(1,id,1);session.release(1,1);session.removed(id) }
        val tracker=PinchDragTracker()
        val motion=IncrementalPetalDrag()
        val origin=Vec(1500f,1000f,2000f)
        fun send(raw:Vec?,down:Boolean=false,up:Boolean=false)=tracker.update(
            PinchDragTracker.Sample(1,down,up,!up,PinchDragTracker.Kind.Remote,if(down)16 else null),
            position={check(!up);raw},grab={id,hand->session.grab(1,id,hand)},
            displacement={_,previous,current->(current-previous)*.001f})
        send(origin,down=true)
        send(origin+Vec(50f)) as PinchDragTracker.Step.Moved
        assertTrue(session.detach(1,16,1))
        val visible=Vec(.06f,.04f,-.01f)
        motion.detach(visible)
        assertEquals(Phase.FinalPetalHeld,session.phase)
        val moved=send(origin+Vec(70f,30f,-10f)) as PinchDragTracker.Step.Moved
        motion.moveBy(moved.frameDelta)
        assertEquals("",session.blessing)
        assertTrue(send(null,up=true) is PinchDragTracker.Step.Released)
        val fallStart=checkNotNull(motion.advance())
        assertVec(visible+Vec(.02f,.03f,-.01f),fallStart)
        assertEquals(PetalState.Falling,session.release(1,1))
        motion.clear()
        assertEquals(Phase.Completing,session.phase)
        session.removed(16)
        assertEquals(Phase.Completed,session.phase)
        assertNull(motion.position)
    }
}
