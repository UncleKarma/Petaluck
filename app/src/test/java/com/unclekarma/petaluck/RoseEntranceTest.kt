package com.unclekarma.petaluck

import com.unclekarma.petaluck.domain.*
import org.junit.Assert.*
import org.junit.Test

class RoseEntranceTest {
    @Test fun entranceKeepsTheSessionLockedUntilFullSizeAndOpacity() {
        val entrance=RoseEntrance()
        val session=RoseSession(1,RoseLayout.assemble(4,30,1))
        val first=entrance.start()
        assertEquals(0f,first.scale,0f);assertEquals(0f,first.opacity,0f)
        repeat(7) {
            val frame=checkNotNull(entrance.advance(.1f))
            assertFalse(frame.finished)
            assertFalse(session.grab(1,0,1))
            assertFalse(session.detach(1,0,1))
            assertEquals(0,session.detachedCount)
        }
        val last=checkNotNull(entrance.advance(.11f))
        assertTrue(last.finished)
        assertEquals(1f,last.scale,0f);assertEquals(1f,last.opacity,0f)
        session.ready()
        assertTrue(session.grab(1,0,1))
        assertTrue(session.detach(1,0,1))
        assertNull(entrance.advance(.1f)) // Completion is emitted once per round.
    }

    @Test fun scaleAndOpacityRiseTogetherWithoutOvershootAcrossFrameRates() {
        for(dt in listOf(1f/90f,1f/60f,1f/30f)) {
            val entrance=RoseEntrance();entrance.start()
            var previous=0f
            var frame=checkNotNull(entrance.advance(dt))
            while(true) {
                assertTrue(frame.scale in previous..1f)
                assertEquals(frame.scale,frame.opacity,0f)
                previous=frame.scale
                if(frame.finished)break
                frame=checkNotNull(entrance.advance(dt))
            }
            assertEquals(1f,previous,0f)
        }
    }

    @Test fun replayOrTeardownNeverReusesAnOldEntranceCompletion() {
        val entrance=RoseEntrance();entrance.start();entrance.advance(.4f)
        entrance.clear()
        assertNull(entrance.advance(10f))
        assertEquals(0f,entrance.start().scale,0f)
        assertFalse(checkNotNull(entrance.advance(.1f)).finished)
        entrance.advance(1f)
        assertEquals(0f,entrance.start().opacity,0f)
    }

    @Test fun invalidOrZeroFrameTimeCannotUnlockInteractionEarly() {
        val entrance=RoseEntrance();entrance.start()
        for(dt in listOf(0f,-1f,Float.NaN,Float.POSITIVE_INFINITY)) {
            val frame=checkNotNull(entrance.advance(dt))
            assertEquals(0f,frame.scale,0f)
            assertFalse(frame.finished)
        }
        assertFalse(checkNotNull(entrance.advance(.4f)).finished)
        assertTrue(checkNotNull(entrance.advance(.4f)).finished)
    }
}
