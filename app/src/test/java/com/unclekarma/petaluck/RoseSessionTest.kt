package com.unclekarma.petaluck

import com.unclekarma.petaluck.domain.*
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class RoseSessionTest {
    @Test fun allCountsAlternateAndFinishWithLoveInAnyOrder() {
        for (n in 17..30) for (seed in 0L..12L) {
            val s = RoseSession(seed, RoseLayout.assemble(seed, n, 0)); s.ready()
            var last = ""
            for (p in s.config.petals.shuffled(Random(seed))) {
                assertTrue(s.grab(seed,p.id,1));assertTrue(s.detach(seed,p.id,1))
                assertFalse(s.detach(seed,p.id,1));assertNotEquals(last,s.result);last=s.result
                s.release(seed,1);s.removed(p.id)
            }
            assertEquals(n,s.detachedCount);assertEquals("喜欢",s.result)
            assertEquals(Phase.Completed,s.phase)
        }
    }
    @Test fun cancelDualHandsOldCallbacksAndPauseDoNotCount() {
        val s=RoseSession(8,RoseLayout.assemble(2,18,3));s.ready()
        assertTrue(s.grab(8,0,11));assertFalse(s.grab(8,1,12));assertFalse(s.detach(8,0,12))
        assertEquals(PetalState.Attached,s.release(8,11));assertEquals(0,s.detachedCount)
        assertFalse(s.grab(7,0,11));assertTrue(s.grab(8,0,11));s.pause();assertFalse(s.detach(8,0,11))
        s.release(8,11);s.resume();assertEquals(0,s.detachedCount)
    }
    @Test fun replayWaitsForTheActualFinalPetalToFinishFalling() {
        val s=RoseSession(1,RoseLayout.assemble(1,17,0));s.ready()
        val order=s.config.petals.reversed()
        val final=order.last().id
        for (p in order) {s.grab(1,p.id,1);s.detach(1,p.id,1);if(p.id!=final)s.release(1,1)}
        s.removed(final);assertEquals(Phase.FinalPetalHeld,s.phase)
        s.release(1,1);assertEquals(Phase.Completing,s.phase)
        order.dropLast(1).forEach {s.removed(it.id)}
        assertEquals(Phase.Completing,s.phase)
        s.removed(final);assertEquals(Phase.Completed,s.phase)
        s.removed(final);assertEquals(Phase.Completed,s.phase)
    }
    @Test fun randomIsReproducibleAndAllCountsAndColorsAreReachable() {
        val counts=mutableSetOf<Int>();val colors=mutableSetOf<Int>();val pairs=mutableSetOf<Pair<Int,Int>>()
        repeat(3000) { seed -> val a=RoseLayout.sample(seed.toLong());assertEquals(a,RoseLayout.sample(seed.toLong()));counts+=a.count;colors+=a.colorId;pairs+=a.count to a.colorId }
        assertEquals((17..30).toSet(),counts);assertEquals((0 until RoseLayout.COLOR_COUNT).toSet(),colors);assertEquals(14 * RoseLayout.COLOR_COUNT,pairs.size)
    }
    @Test fun invalidAssetsFailBeforePlayAndEvenCountsAreValid() {
        RoseLayout.assemble(1,18,0).validate();RoseLayout.assemble(1,30,7).validate()
        for (bad in listOf(16,31)) assertThrows(IllegalArgumentException::class.java) {RoseLayout.assemble(1,bad,0)}
        val c=RoseLayout.assemble(1,17,0)
        assertThrows(IllegalArgumentException::class.java) {c.copy(petals=c.petals.drop(1)).validate()}
        assertThrows(IllegalArgumentException::class.java) {c.copy(colorId=RoseLayout.COLOR_COUNT).validate()}
        assertThrows(IllegalArgumentException::class.java) {c.copy(petals=c.petals.map {it.copy(id=0)}).validate()}
        assertThrows(IllegalArgumentException::class.java) {c.validate(emptySet())}
    }
    @Test fun detachUsesDistanceInEveryDirectionAndKeepsTheThreshold() {
        // Cover both signs on every axis and diagonal pulls, including inward/backward.
        for (x in -1..1) for (y in -1..1) for (z in -1..1) {
            val direction=Vec(x.toFloat(),y.toFloat(),z.toFloat())
            if(direction.length()==0f)continue
            val unit=direction*(1f/direction.length())
            assertTrue(PullRules.distance(unit*.039f)<PullRules.THRESHOLD)
            assertTrue(PullRules.distance(unit*.041f)>=PullRules.THRESHOLD)
        }
        assertEquals(PullRules.THRESHOLD,PullRules.distance(Vec(.04f)),.000001f)
        assertEquals(0f,PullRules.distance(Vec()),0f)
        assertTrue(PullRules.distance(Vec(0f,.025f,.025f))<PullRules.THRESHOLD)
        assertTrue(PullRules.distance(Vec(.025f,.025f,.025f))>=PullRules.THRESHOLD)
        assertTrue(PullRules.DEAD_ZONE<PullRules.THRESHOLD)
    }
    @Test fun twentyConsecutiveRoundsRejectOldCallbacksAndKeepRealEntityCounts() {
        var previous:RoseSession?=null
        repeat(20) {round->
            val s=RoseSession(round.toLong()+100,RoseLayout.sample(round.toLong()));s.ready()
            previous?.let{assertFalse(s.grab(it.id,0,1));assertFalse(s.detach(it.id,0,1))}
            assertEquals(s.config.count,s.config.petals.size)
            s.config.petals.forEach{assertTrue(s.grab(s.id,it.id,1));assertTrue(s.detach(s.id,it.id,1));s.release(s.id,1);s.removed(it.id)}
            assertEquals(Phase.Completed,s.phase);assertEquals("喜欢",s.result);previous=s
        }
    }
}
