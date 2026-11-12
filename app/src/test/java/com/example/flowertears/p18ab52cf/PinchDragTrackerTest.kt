package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.domain.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class PinchDragTrackerTest {
    @Test fun remoteAnchorWaitsForValidMoveAndSurvivesDuplicateDownWithoutRebasing() {
        val tracker = PinchDragTracker()
        var conversions = 0
        var grabs = 0
        fun send(raw: Vec?, down: Boolean = false, up: Boolean = false) = tracker.update(
            PinchDragTracker.Sample(1, down, up, !up, PinchDragTracker.Kind.Remote, if (down) 0 else null),
            position = { raw }, grab = { _, _ -> grabs++; true },
            displacement = { _, previous, current -> conversions++; (current - previous) * .001f })
        assertTrue(send(Vec(1000f), down = true) is PinchDragTracker.Step.Started)
        assertNull(send(Vec(1200f), down = true))
        assertTrue(send(null) is PinchDragTracker.Step.Unavailable)
        assertTrue(send(Vec(Float.NaN)) is PinchDragTracker.Step.Unavailable)
        assertTrue(send(Vec(1500f)) is PinchDragTracker.Step.Anchored)
        assertEquals(0, conversions)
        assertNull(send(Vec(3000f), down = true))
        assertVec(Vec(.01f), (send(Vec(1510f)) as PinchDragTracker.Step.Moved).delta)
        assertVec(Vec(.02f), (send(Vec(1520f)) as PinchDragTracker.Step.Moved).delta)
        assertEquals(1, grabs)
        assertTrue(send(null, up = true) is PinchDragTracker.Step.Released)
        assertTrue(send(Vec(6000f), down = true) is PinchDragTracker.Step.Started)
        assertTrue(send(Vec(9000f)) is PinchDragTracker.Step.Anchored)
        assertVec(Vec(.005f), (send(Vec(9005f)) as PinchDragTracker.Step.Moved).delta)
    }

    @Test fun pendingRemoteCaptureUsesItsFirstMoveAndUnanchoredReleaseNeedsNoPose() {
        val h = Harness()
        assertTrue(h.send(kind=PinchDragTracker.Kind.Remote,down=true,device=Vec(1000f)) is PinchDragTracker.Step.Started)
        val reads = h.sources.size
        assertTrue(h.send(up=true,device=null) is PinchDragTracker.Step.Released)
        assertEquals(reads, h.sources.size)
        assertEquals(0, h.session.detachedCount)
        assertNull(h.send(kind=PinchDragTracker.Kind.Other,down=true,target=1))
        val start = h.send(kind=PinchDragTracker.Kind.Remote,target=2,device=Vec(.5f)) as PinchDragTracker.Step.Started
        assertEquals(1, start.petal)
        val moved = h.send(kind=PinchDragTracker.Kind.Other,target=null,device=Vec(.52f)) as PinchDragTracker.Step.Moved
        assertVec(Vec(.02f), moved.delta)
        assertEquals(0, h.session.detachedCount)
    }

    @Test fun recordedRemoteDownToFirstMoveDiscontinuityDoesNotTearOrLoadTheStem() {
        val tracker = PinchDragTracker()
        val drag = RemotePetalDrag().apply { start() }
        val stem = StemMotion().apply { grab() }
        val strengths = mutableListOf<Float>()
        var tears = 0
        val tearPose = Vec(.06f, .03f, -.01f)
        fun send(raw: Vec, down: Boolean = false): Vec? {
            val step = tracker.update(
                PinchDragTracker.Sample(1, down, false, true, PinchDragTracker.Kind.Remote, if (down) 0 else null),
                position = { raw }, grab = { _, _ -> true },
                displacement = { _, previous, current ->
                    val d = (current - previous) * .0004f
                    Vec(d.x, -d.y, d.z)
                })
            if (step is PinchDragTracker.Step.Moved) drag.moveBy(step.frameDelta)
            return drag.advance { delta, crossing ->
                val strength = PullRules.strength(delta.length())
                strengths += strength
                stem.pull(delta, strength)
                if (crossing) { tears++; tearPose } else null
            }
        }
        send(Vec(712.55927f, 997.3746f, 4855.635f), down = true)
        val firstMove = Vec(1213.1f, 1226.1869f, 4079.283f)
        assertNull(send(firstMove))
        assertEquals(0, tears)
        assertTrue(strengths.isEmpty())
        // The next real recorded sample is sub-millimeter, not another large jump.
        assertNull(send(Vec(1215.0251f, 1226.0284f, 4080.0034f)))
        assertEquals(0f, strengths.last(), .000001f)
        assertNull(send(firstMove + Vec(50f)))
        assertTrue(strengths.last() in .01f..0.99f)
        assertEquals(0, tears)
        val held = checkNotNull(send(firstMove + Vec(102.5f)))
        assertEquals(1, tears)
        assertVec(tearPose + Vec(.001f), held)
        assertVec(held, checkNotNull(send(firstMove + Vec(102.5f))))
        assertVec(held + Vec(.02f), checkNotNull(send(firstMove + Vec(152.5f))))
        assertEquals(1, tears)
    }

    @Test fun remoteAccumulationDoesNotRescaleEarlierMovementWhenConversionChanges() {
        val tracker = PinchDragTracker()
        var scale = .001f
        fun send(raw: Vec, down: Boolean = false) = tracker.update(
            PinchDragTracker.Sample(1, down, false, true, PinchDragTracker.Kind.Remote, if (down) 0 else null),
            position = { raw }, grab = { _, _ -> true },
            displacement = { _, previous, current -> (current - previous) * scale })
        send(Vec(1000f), down = true)
        assertTrue(send(Vec(1000f)) is PinchDragTracker.Step.Anchored)
        val first = send(Vec(1010f)) as PinchDragTracker.Step.Moved
        assertVec(Vec(.01f), first.delta)
        scale = .003f
        val stationary = send(Vec(1010f)) as PinchDragTracker.Step.Moved
        assertVec(Vec(), stationary.frameDelta)
        assertVec(Vec(.01f), stationary.delta)
        val next = send(Vec(1015f)) as PinchDragTracker.Step.Moved
        assertVec(Vec(.015f), next.frameDelta)
        assertVec(Vec(.025f), next.delta)
    }

    private fun assertVec(expected:Vec,actual:Vec) {
        assertEquals(expected.x,actual.x,.000001f)
        assertEquals(expected.y,actual.y,.000001f)
        assertEquals(expected.z,actual.z,.000001f)
    }

    @Test fun remoteDetachKeepsItsPoseWhenTheViewOriginChangesThenFollowsRelativeHandMotion() {
        val tracker=PinchDragTracker()
        val motion=IncrementalPetalDrag()
        val rawOrigin=Vec(1800f,900f,1500f)
        var viewOrigin=Vec(1000f,800f,1000f)
        val capturedOrigins=mutableListOf<Vec>()
        fun send(raw:Vec,down:Boolean=false,kind:PinchDragTracker.Kind=PinchDragTracker.Kind.Remote) =
            tracker.update(PinchDragTracker.Sample(1,down,false,true,kind,if(down)0 else null),
                position={source->assertEquals(PinchDragTracker.PositionSource.GlobalDevicePose,source);raw},
                displacement={_,origin,current->
                    capturedOrigins+=origin
                    // SDK convertSize models the pixel->meter scale, without the changing view origin.
                    val delta=current-origin
                    Vec(delta.x,-delta.y,delta.z)*.001f
                },grab={_,_->true})
        fun convertedPoint(raw:Vec):Vec {
            val p=raw-viewOrigin
            return Vec(p.x,-p.y,p.z)*.001f
        }
        assertTrue(send(rawOrigin,down=true) is PinchDragTracker.Step.Started)
        assertTrue(send(rawOrigin) is PinchDragTracker.Step.Anchored)
        val rawAtTear=rawOrigin+Vec(41f,-8f,12f)
        val tear=send(rawAtTear) as PinchDragTracker.Step.Moved
        assertVec(Vec(.041f,.008f,.012f),tear.delta)
        val visibleAtTear=Vec(.062f,.034f,-.015f)
        motion.detach(visibleAtTear)
        val oldPoint=convertedPoint(rawAtTear)
        // A layout/global-origin change must not become movement of an already detached petal.
        viewOrigin=Vec(620f,1300f,1600f)
        assertTrue((convertedPoint(rawAtTear)-oldPoint).length()>.5f)
        val stationary=send(rawAtTear,kind=PinchDragTracker.Kind.Other) as PinchDragTracker.Step.Moved
        motion.moveBy(stationary.frameDelta)
        assertVec(visibleAtTear,checkNotNull(motion.advance()))
        val moved=send(rawAtTear+Vec(240f,70f,-130f)) as PinchDragTracker.Step.Moved
        motion.moveBy(moved.frameDelta)
        assertVec(visibleAtTear+Vec(.24f,-.07f,-.13f),checkNotNull(motion.advance()))
        assertEquals(listOf(rawOrigin,rawAtTear,rawAtTear),capturedOrigins)
    }

    @Test fun remoteRawPixelDisplacementsHaveTheSameAnyDirectionMeterThresholdAsDirectPinch() {
        for(x in -1..1)for(y in -1..1)for(z in -1..1) {
            if(x==0 && y==0 && z==0)continue
            val tracker=PinchDragTracker()
            val origin=Vec(2300f,1700f,800f)
            val direction=Vec(x.toFloat(),y.toFloat(),z.toFloat())*(1/sqrt((x*x+y*y+z*z).toFloat()))
            fun sample(distance:Float,down:Boolean=false)=tracker.update(
                PinchDragTracker.Sample(1,down,false,true,PinchDragTracker.Kind.Remote,if(down)0 else null),
                position={origin+direction*(distance/.002f)},
                displacement={_,start,current->val d=(current-start)*.002f;Vec(d.x,-d.y,d.z)},
                grab={_,_->true})
            sample(0f,down=true)
            assertTrue(sample(0f) is PinchDragTracker.Step.Anchored)
            val before=sample(.039f) as PinchDragTracker.Step.Moved
            val after=sample(.041f) as PinchDragTracker.Step.Moved
            assertTrue(PullRules.distance(before.delta)<PullRules.THRESHOLD)
            assertTrue(PullRules.distance(after.delta)>PullRules.THRESHOLD)
        }
    }

    @Test fun invalidDisplacementConversionKeepsTheOriginalCaptureForRecoveryAndPoseFreeRelease() {
        val tracker=PinchDragTracker()
        val origin=Vec(1000f,2000f,1000f)
        var valid=true
        var reads=0
        fun sample(raw:Vec,down:Boolean=false,up:Boolean=false)=tracker.update(
            PinchDragTracker.Sample(1,down,up,!up,PinchDragTracker.Kind.Remote,if(down)0 else null),
            position={reads++;raw},
            displacement={_,start,current->if(valid)(current-start)*.001f else Vec(Float.NaN)},
            grab={_,_->true})
        sample(origin,down=true)
        assertTrue(sample(origin) is PinchDragTracker.Step.Anchored)
        valid=false
        assertTrue(sample(origin+Vec(30f)) is PinchDragTracker.Step.Unavailable)
        valid=true
        val recovered=sample(origin+Vec(50f)) as PinchDragTracker.Step.Moved
        assertVec(Vec(.05f),recovered.delta)
        val beforeRelease=reads
        assertTrue(sample(Vec(Float.NaN),up=true) is PinchDragTracker.Step.Released)
        assertEquals(beforeRelease,reads)
    }

    @Test fun finalDetachedPetalFollowsBothPinchesUntilReleaseWithoutPoseOnUp() {
        for(kind in listOf(PinchDragTracker.Kind.Direct,PinchDragTracker.Kind.Remote)) {
            val h=Harness()
            for(id in 0 until 16) {
                h.session.grab(1,id,1);h.session.detach(1,id,1)
                h.session.release(1,1);h.session.removed(id)
            }
            val direct=kind==PinchDragTracker.Kind.Direct
            val origin=Vec(.125f,.25f,.5f)
            fun send(delta:Vec,down:Boolean=false,target:Int?=null,metadata:PinchDragTracker.Kind=kind) = h.send(
                kind=metadata,down=down,target=target,
                local=if(direct)delta else Vec(99f),device=if(direct)null else origin+delta)
            assertTrue(send(Vec(),down=true,target=16) is PinchDragTracker.Step.Started)
            if(!direct)assertTrue(send(Vec()) is PinchDragTracker.Step.Anchored)
            val motion=PetalDragMotion()
            val tear=send(Vec(.041f)) as PinchDragTracker.Step.Moved
            motion.move(tear.delta);motion.detach(Vec(.065f,.04f,-.005f))
            assertEquals(Phase.FinalPetalHeld,h.session.phase)
            assertNull(h.send(id=2,down=true,target=0)) // No second-hand takeover.
            for(delta in listOf(Vec(.26f,.09f),Vec(-.17f,.12f,.08f),Vec(.015f))) {
                val moved=send(delta,metadata=PinchDragTracker.Kind.Other) as PinchDragTracker.Step.Moved
                motion.move(moved.delta)
                val expected=Vec(.065f,.04f,-.005f)+delta-tear.delta
                val actual=checkNotNull(motion.detachedPosition)
                assertEquals(expected.x,actual.x,.000001f)
                assertEquals(expected.y,actual.y,.000001f)
                assertEquals(expected.z,actual.z,.000001f)
                assertEquals(PetalState.DetachedHeld,h.session.state(16))
                assertEquals(17,h.session.detachedCount)
                assertEquals(Phase.FinalPetalHeld,h.session.phase)
                assertEquals("",h.session.blessing)
            }
            val releasedPosition=checkNotNull(motion.detachedPosition)
            val reads=h.sources.size
            assertTrue(h.send(kind=PinchDragTracker.Kind.Other,up=true,local=null,device=null,target=null) is PinchDragTracker.Step.Released)
            assertEquals(reads,h.sources.size)
            assertEquals(PetalState.Falling,h.session.state(16))
            assertEquals(Phase.Completing,h.session.phase)
            motion.clear()
            assertNull(send(Vec(.4f)));assertNull(motion.detachedPosition)
            assertEquals(.039f,releasedPosition.x,.000001f)
            h.session.removed(16);assertEquals(Phase.Completed,h.session.phase)
        }
    }

    private class Harness {
        val tracker = PinchDragTracker()
        val session = RoseSession(1, RoseLayout.assemble(20261110, 17, 4)).apply { ready() }
        val sources = mutableListOf<PinchDragTracker.PositionSource>()
        fun send(kind: PinchDragTracker.Kind = PinchDragTracker.Kind.Direct,
            id: Long = 1, target: Int? = 0, down: Boolean = false, up: Boolean = false,
            pressed: Boolean = !up, local: Vec? = Vec(), device: Vec? = null): PinchDragTracker.Step? {
            val step = tracker.update(PinchDragTracker.Sample(id, down, up, pressed, kind, target),
                position = { source -> sources += source
                    if (source == PinchDragTracker.PositionSource.LocalPointer) local else device },
                grab = { petal, pointer -> session.grab(session.id, petal, pointer) })
            when (step) {
                is PinchDragTracker.Step.Moved -> if (PullRules.distance(step.delta) >= PullRules.THRESHOLD)
                    session.detach(session.id, checkNotNull(session.heldId), step.pointerId)
                is PinchDragTracker.Step.Released -> session.release(session.id, step.pointerId)
                else -> Unit
            }
            return step
        }
    }

    @Test fun directPinchUsesLocal3dWithoutDevicePoseAndSurvivesDetachUntilUp() {
        val h=Harness()
        assertTrue(h.send(down=true) is PinchDragTracker.Step.Started) // Zero is a valid local point.
        h.send(local=Vec(.039f),device=Vec(10f,10f,10f));assertEquals(0,h.session.detachedCount)
        h.send(local=Vec(.041f),target=null);assertEquals(1,h.session.detachedCount)
        h.send(kind=PinchDragTracker.Kind.Other,local=Vec(.06f),target=null)
        assertEquals(PetalState.DetachedHeld,h.session.state(0))
        val reads=h.sources.size
        h.send(kind=PinchDragTracker.Kind.Other,up=true,local=null,target=null)
        assertEquals(reads,h.sources.size) // Up need not carry pose or spatial metadata.
        assertEquals(PetalState.Falling,h.session.state(0));assertEquals(1,h.session.detachedCount)
        assertTrue(h.sources.all {it==PinchDragTracker.PositionSource.LocalPointer})
    }

    @Test fun gazePinchKeepsDeviceMotionAndIgnoresProjectedPointerMotion() {
        val h=Harness();val origin=Vec(.1f,.2f,.3f)
        h.send(kind=PinchDragTracker.Kind.Remote,down=true,device=origin)
        h.send(local=Vec(10f,10f,10f),device=origin) // Kind can change; capture source must not.
        assertEquals(0,h.session.detachedCount)
        h.send(kind=PinchDragTracker.Kind.Remote,local=Vec(),device=origin+Vec(0f,0f,.041f))
        assertEquals(1,h.session.detachedCount)
        assertTrue(h.sources.all {it==PinchDragTracker.PositionSource.GlobalDevicePose})
    }

    @Test fun directAndGazeDragsLoadTheSameStemEvenWhenOneSampleCrossesTheThreshold() {
        fun bendsFor(kind: PinchDragTracker.Kind, gradual: Boolean): List<Vec> {
            val h = Harness()
            val stem = StemMotion()
            val origin = Vec(.125f, .25f, .5f)
            val direct = kind == PinchDragTracker.Kind.Direct
            fun sample(delta: Vec, down: Boolean = false) = h.send(
                kind = kind, down = down,
                local = if (direct) delta else Vec(100f),
                device = if (direct) null else origin + delta
            )
            assertTrue(sample(Vec(), down = true) is PinchDragTracker.Step.Started)
            if (!direct) assertTrue(sample(Vec()) is PinchDragTracker.Step.Anchored)
            stem.grab()
            fun pull(delta: Vec): Vec {
                val moved = sample(delta) as PinchDragTracker.Step.Moved
                return stem.pull(moved.delta, PullRules.strength(PullRules.distance(moved.delta)))
            }
            val beforeDetach = if (gradual) pull(Vec(.015625f, 0f, -.015625f)) else Vec()
            assertEquals(0, h.session.detachedCount)
            if (gradual) assertTrue(beforeDetach.length() in .01f..0.99f)
            val loaded = pull(Vec(.0625f, 0f, -.0625f))
            assertEquals(1, h.session.detachedCount)
            stem.release(rebound = true)
            val firstFrame = checkNotNull(stem.advance(.016f))
            val reverse = checkNotNull(stem.advance(.204f))
            assertTrue(firstFrame.x > .5f)
            assertTrue(reverse.x < 0f)
            return listOf(beforeDetach, loaded, firstFrame, reverse)
        }
        for (gradual in listOf(false, true)) {
            assertEquals(bendsFor(PinchDragTracker.Kind.Direct, gradual), bendsFor(PinchDragTracker.Kind.Remote, gradual))
        }
    }

    @Test fun pokeOrMissingKindCanBecomeDirectPinchWithoutChangingTheOriginalPetal() {
        val h=Harness()
        assertNull(h.send(kind=PinchDragTracker.Kind.Other,down=true,target=0))
        assertNull(h.send(kind=PinchDragTracker.Kind.Other,local=Vec(.06f),target=1))
        assertNull(h.session.heldId) // Poke/movement alone cannot pluck.
        val start=h.send(local=Vec(.06f),target=1) as PinchDragTracker.Step.Started
        assertEquals(0,start.petal)
        h.send(local=Vec(.101f),target=1)
        assertEquals(1,h.session.detachedCount);assertEquals(PetalState.DetachedHeld,h.session.state(0))
        assertEquals(PetalState.Attached,h.session.state(1))
    }

    @Test fun invalidDirectSampleDoesNotSwitchToDevicePoseAndCanRecover() {
        val h=Harness()
        assertNull(h.send(down=true,local=null))
        assertTrue(h.send(local=Vec()) is PinchDragTracker.Step.Started)
        assertTrue(h.send(local=Vec(Float.NaN),device=Vec(10f)) is PinchDragTracker.Step.Unavailable)
        assertTrue(h.send(local=null,device=Vec(10f)) is PinchDragTracker.Step.Unavailable)
        val recovered=h.send(local=Vec(.02f)) as PinchDragTracker.Step.Moved
        assertEquals(.02f,recovered.delta.x,.000001f);assertEquals(0,h.session.detachedCount)
    }

    @Test fun otherHandAlreadyDownMustReleaseAndPinchAgain() {
        val h=Harness()
        h.send(down=true,id=1,target=0)
        assertNull(h.send(down=true,id=2,target=1))
        h.send(up=true,id=1)
        assertNull(h.send(id=2,target=1,local=Vec(.1f)))
        assertNull(h.session.heldId)
        h.send(up=true,id=2)
        assertTrue(h.send(down=true,id=2,target=1) is PinchDragTracker.Step.Started)
        assertEquals(1,h.session.heldId)
    }

    @Test fun pendingOtherHandCannotWakeAfterTheActiveHandReleases() {
        val h=Harness()
        h.send(kind=PinchDragTracker.Kind.Other,down=true,id=2,target=1)
        h.send(down=true,id=1,target=0)
        h.send(up=true,id=1)
        assertNull(h.send(id=2,target=1));assertNull(h.session.heldId)
    }

    @Test fun rejectionClearAndCancellationRequireAFreshDown() {
        val h=Harness();h.session.pause()
        assertNull(h.send(down=true))
        h.session.resume();assertNull(h.send(local=Vec(.1f)))
        h.send(down=true)
        val cancel=h.send(pressed=false,up=false) as PinchDragTracker.Step.Released
        assertTrue(cancel.cancelled);assertEquals(PetalState.Attached,h.session.state(0))
        h.send(down=true)
        h.tracker.clear();h.session.release(h.session.id,1)
        assertNull(h.send(local=Vec(.1f)));assertNull(h.session.heldId)
    }

    @Test fun directDragAll26DirectionsAndDuplicateDownsKeepOneOriginAndOneCount() {
        for(x in -1..1)for(y in -1..1)for(z in -1..1) {
            if(x==0 && y==0 && z==0)continue
            val h=Harness();val length=sqrt((x*x+y*y+z*z).toFloat());val direction=Vec(x.toFloat(),y.toFloat(),z.toFloat())*(1/length)
            h.send(down=true)
            h.send(local=direction*.02f)
            h.send(down=true,local=direction*.039f) // Must not re-zero the origin.
            assertEquals(0,h.session.detachedCount)
            h.send(local=direction*.041f);h.send(local=direction*.06f)
            assertEquals(1,h.session.detachedCount)
            h.send(up=true);assertEquals(PetalState.Falling,h.session.state(0))
        }
    }
}
