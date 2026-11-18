package com.unclekarma.petaluck

import com.unclekarma.petaluck.domain.*
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class RoseBlessingsTest {
    @Test fun onlyColorfulFinaleHidesTheResultAsSoonAsTheLastPetalDetaches() {
        for (color in listOf(0, RoseLayout.MULTICOLOR)) {
            val session = RoseSession(1, RoseLayout.assemble(7, 18, color)) { RoseBlessings.COLORFUL_FINALE }
            session.ready()
            for ((index, petal) in session.config.petals.withIndex()) {
                assertTrue(session.grab(session.id, petal.id, 1))
                assertTrue(session.detach(session.id, petal.id, 1))
                val last = index == session.config.petals.lastIndex
                val expected = if (last && color == RoseLayout.MULTICOLOR) "" else session.result
                assertEquals(expected, session.displayedResult)
                session.release(session.id, 1)
                assertEquals(expected, session.displayedResult)
                session.removed(petal.id)
                assertEquals(expected, session.displayedResult)
            }
        }
    }

    @Test fun chooseOnlyOnFinalReleaseAndKeepTheSameWishThroughCompletion() {
        var draws=0
        val expected=RoseBlessings.messages[2]
        val s=RoseSession(1,RoseLayout.assemble(7,18,3)) {draws++;expected}
        s.ready()
        for(p in s.config.petals) {
            assertTrue(s.grab(s.id,p.id,1));assertTrue(s.detach(s.id,p.id,1))
            assertEquals("",s.blessing);assertEquals(0,draws)
            s.release(s.id,1)
            if(p.id!=s.config.petals.last().id)assertEquals("",s.blessing)
        }
        assertEquals(Phase.Completing,s.phase);assertEquals(expected,s.blessing);assertEquals(1,draws)
        repeat(10) {s.release(s.id,1);s.pause();s.resume()}
        s.removed(s.config.petals.last().id)
        assertEquals(Phase.Completed,s.phase);assertEquals(expected,s.blessing);assertEquals(1,draws)
        assertEquals(18,s.detachedCount);assertEquals("喜欢",s.result)
    }

    @Test fun replayStartsWithoutPreviousWishAndSamplesIndependently() {
        var draws=0
        fun round(id:Long)=RoseSession(id,RoseLayout.assemble(id,17,0)) {RoseBlessings.messages[draws++]}
        fun finish(s:RoseSession) {
            s.ready()
            s.config.petals.forEach {s.grab(s.id,it.id,1);s.detach(s.id,it.id,1);s.release(s.id,1)}
            s.removed(s.config.petals.last().id)
        }
        val a=round(1);finish(a)
        val b=round(2)
        assertEquals("",b.blessing);assertEquals(1,draws)
        finish(b)
        assertEquals(RoseBlessings.messages[0],a.blessing)
        assertEquals(RoseBlessings.messages[1],b.blessing);assertEquals(2,draws)
    }

    @Test fun wishesAreDistinctShortAndReachableWithReproducibleSampling() {
        assertTrue(RoseBlessings.messages.size>=10)
        assertEquals(RoseBlessings.messages.size,RoseBlessings.messages.toSet().size)
        // At 80sp in a 560dp panel, these short Chinese wishes fit within two base-size lines.
        assertTrue(RoseBlessings.messages.all {it.isNotBlank() && it.length<=14})
        val samples=(0..1000).map {seed->
            val a=RoseBlessings.sample(Random(seed))
            assertEquals(a,RoseBlessings.sample(Random(seed)))
            a
        }.toSet()
        assertEquals(RoseBlessings.messages.toSet(),samples)
    }

    @Test fun allWishesAndRepeatedRoundsReachReplayAfterTheFinalFall() {
        val colors=RecentRoseColors()
        repeat(36) {round->
            val wish=RoseBlessings.messages[round%RoseBlessings.messages.size]
            val config=colors.sample(round.toLong())
            val s=RoseSession(round.toLong(),config) {wish}
            s.ready();colors.recordReady(config.colorId)
            val petals=config.petals.shuffled(Random(round))
            for (petal in petals) {
                assertTrue(s.grab(s.id,petal.id,1))
                assertTrue(s.detach(s.id,petal.id,1))
                s.release(s.id,1)
            }
            assertEquals(Phase.Completing,s.phase)
            assertEquals(wish,s.blessing)
            s.pause();s.resume() // Host pauses can interrupt the fall.
            petals.dropLast(1).forEach {s.removed(it.id)}
            assertEquals(Phase.Completing,s.phase)
            s.removed(petals.last().id)
            assertEquals(Phase.Completed,s.phase)
            assertEquals(wish,s.blessing)
        }
    }
}
