package com.unclekarma.petaluck

import com.unclekarma.petaluck.scene.SceneFrameGate
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class SceneFrameGateTest {
    @Test fun newlyAttachedAudioOwnerWaitsForANewFrameRatherThanAnOldOne()=runBlocking {
        val gate=SceneFrameGate()
        repeat(5) {gate.onFrame()}
        var prepared=false
        val job=launch(start=CoroutineStart.UNDISPATCHED) {gate.awaitNextFrame();prepared=true}
        repeat(20) {yield()}
        assertFalse(prepared)
        gate.onFrame();job.join()
        assertTrue(prepared)
    }

    @Test fun teardownCancelsPendingAudioPreparationBeforeTheNextSceneFrame()=runBlocking {
        val gate=SceneFrameGate();gate.onFrame()
        var prepared=false
        val job=launch(start=CoroutineStart.UNDISPATCHED) {gate.awaitNextFrame();prepared=true}
        job.cancelAndJoin();gate.onFrame();yield()
        assertFalse(prepared)
    }

    @Test fun coldStartupDoesNotLoadAssetsBeforeTheSpatialViewProducesAFrame()=runBlocking {
        val gate=SceneFrameGate()
        var loaded=false
        val job=launch(start=CoroutineStart.UNDISPATCHED) {gate.awaitFirstFrame();loaded=true}
        repeat(20) {yield()}
        assertFalse(loaded)
        gate.onFrame();job.join()
        assertTrue(loaded)
    }

    @Test fun slowColdRigCanBecomeReadyAfterTheOldSixtyPollLimit()=runBlocking {
        val gate=SceneFrameGate()
        var nativeReady=false
        var reads=0
        val job=async(start=CoroutineStart.UNDISPATCHED) {
            gate.awaitReady {reads++;if(nativeReady)"three-joint pose" else null}
        }
        assertEquals(0,reads)
        repeat(120) {gate.onFrame();yield()}
        assertFalse(job.isCompleted)
        nativeReady=true;gate.onFrame()
        assertEquals("three-joint pose",job.await())
    }

    @Test fun noFramesDoesNotConsumeReadinessBudgetOrPollNativeState()=runBlocking {
        val gate=SceneFrameGate();gate.onFrame()
        var ready=false
        var reads=0
        val job=async(start=CoroutineStart.UNDISPATCHED) {gate.awaitReady(maxFrames=3) {reads++;if(ready)1 else null}}
        repeat(100) {yield()}
        assertEquals(1,reads);assertFalse(job.isCompleted)
        ready=true;gate.onFrame();assertEquals(1,job.await())
    }

    @Test fun aFrameBeforeSubscriptionIsNotLostAndReadyStateReturnsImmediately()=runBlocking {
        val gate=SceneFrameGate();gate.onFrame()
        gate.awaitFirstFrame()
        assertEquals("ready",gate.awaitReady {"ready"})
    }

    @Test fun anInvalidRigStillFailsAfterTheActualFrameBudget()=runBlocking {
        val gate=SceneFrameGate();gate.onFrame()
        var failure:IllegalStateException?=null
        val job=launch(start=CoroutineStart.UNDISPATCHED) {
            try {gate.awaitReady<String>(maxFrames=3) {null};fail("Expected readiness failure")}
            catch(e:IllegalStateException) {failure=e}
        }
        repeat(3) {gate.onFrame();yield()}
        job.join()
        assertTrue(failure?.message?.contains("3 active frames")==true)
    }

    @Test fun disposingStartupCancelsTheWaitWithoutLoadingOnALaterFrame()=runBlocking {
        val gate=SceneFrameGate()
        var loads=0
        val job=launch(start=CoroutineStart.UNDISPATCHED) {gate.awaitFirstFrame();loads++}
        job.cancelAndJoin();gate.onFrame();yield()
        assertEquals(0,loads)
    }
}
