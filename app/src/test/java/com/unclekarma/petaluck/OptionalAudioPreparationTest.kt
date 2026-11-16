package com.unclekarma.petaluck

import com.unclekarma.petaluck.domain.Phase
import com.unclekarma.petaluck.domain.RoseLayout
import com.unclekarma.petaluck.domain.RoseSession
import com.unclekarma.petaluck.scene.prepareOptionalAudio
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class OptionalAudioPreparationTest {
    @Test fun nativeLoadFailureCannotClearAPlayableFlower() = runBlocking {
        val session = RoseSession(1, RoseLayout.assemble(10, 30, 1)).apply { ready() }
        val failure = IOException("Native audio resource could not be loaded")
        var reported: Exception? = null
        val prepared = prepareOptionalAudio(prepare = { throw failure }, onFailure = { reported = it })
        assertFalse(prepared)
        assertSame(failure, reported)
        assertEquals(Phase.Ready, session.phase)
        // The whole round, including its final petal and replay state, must remain usable.
        for (petal in session.config.petals) {
            assertTrue(session.grab(session.id, petal.id, 1))
            assertTrue(session.detach(session.id, petal.id, 1))
            session.release(session.id, 1)
            session.removed(petal.id)
        }
        assertEquals(Phase.Completed, session.phase)
    }

    @Test fun repeatedInvalidControllersDoNotTrapReplayInTheErrorState() = runBlocking {
        var failures = 0
        repeat(4) { round ->
            val session = RoseSession(round.toLong(), RoseLayout.sample(round.toLong())).apply { ready() }
            assertFalse(prepareOptionalAudio(
                prepare = { throw IllegalStateException("Cannot prepare petal sound") },
                onFailure = { failures++ },
            ))
            assertEquals(Phase.Ready, session.phase)
            assertTrue(session.grab(session.id, 0, 1))
            assertTrue(session.detach(session.id, 0, 1))
            assertEquals(1, session.detachedCount)
        }
        assertEquals(4, failures)
    }

    @Test fun roundCancellationStillPropagatesWithoutBecomingAnAudioError() = runBlocking {
        val cancelled = CancellationException("Round was replaced")
        var reported = false
        try {
            prepareOptionalAudio(prepare = { throw cancelled }, onFailure = { reported = true })
            fail("Expected cancellation")
        } catch (e: CancellationException) {
            assertSame(cancelled, e)
        }
        assertFalse(reported)
    }
}
