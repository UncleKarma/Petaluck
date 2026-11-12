package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.domain.Phase
import com.example.flowertears.p18ab52cf.domain.RoseLayout
import com.example.flowertears.p18ab52cf.domain.RoseSession
import com.example.flowertears.p18ab52cf.scene.EndingMusicPlayback
import com.example.flowertears.p18ab52cf.scene.PcmWaveClip
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class EndingMusicTest {
    private class Output {
        var starts = 0
        var stops = 0
        var gain = -1f
        val player = EndingMusicPlayback({ starts++ }, { gain = it }, { stops++ })
    }

    @Test fun lastReleaseStartsMusicWithBlessingOnlyOnceAcrossCompletion() {
        val session = RoseSession(1, RoseLayout.sample(123))
        val output = Output()
        session.ready()
        for (petal in session.config.petals) {
            session.grab(session.id, petal.id, 4)
            session.detach(session.id, petal.id, 4)
            assertNotEquals(Phase.Completing, session.phase)
            assertEquals(0, output.starts)
            session.release(session.id, 4)
            if (session.phase == Phase.Completing) {
                assertTrue(session.blessing.isNotEmpty())
                output.player.blessing(session.id, 0, 14500)
            }
        }
        assertEquals(1, output.starts)
        session.removed(session.config.petals.last().id)
        assertEquals(Phase.Completed, session.phase)
        output.player.blessing(session.id, 300, 14500)
        assertEquals(1, output.starts)
    }

    @Test fun replayFadesForOneSecondWhileNextRoundCanLoad() {
        val output = Output()
        output.player.blessing(1, 0, 14500)
        output.player.replay(2000)
        // Wall-clock updates continue without scene frames or ownership by the old flower.
        output.player.tick(2000, false)
        assertEquals(1f, output.gain, 0f)
        output.player.tick(2500, false)
        assertEquals(.5f, output.gain, .0001f)
        output.player.replay(2600) // Duplicate clicks do not extend the fade.
        output.player.tick(2999, false)
        assertTrue(output.player.active)
        output.player.tick(3000, false)
        assertEquals(0f, output.gain, 0f)
        assertFalse(output.player.active)
        assertEquals(1, output.stops)
    }

    @Test fun naturallyEndedMusicDoesNotRestartOnReplay() {
        val output = Output()
        output.player.blessing(1, 0, 14500)
        output.player.tick(14500, true)
        output.player.replay(15000)
        output.player.tick(15500, false)
        assertFalse(output.player.active)
        assertEquals(1, output.starts)
        assertEquals(1, output.stops)
    }

    @Test fun endDuringFadeStopsWithoutWaitingForTheRemainingSecond() {
        val output = Output()
        output.player.blessing(1, 0, 14500)
        output.player.replay(14000)
        output.player.tick(14200, false)
        output.player.tick(14500, true)
        assertFalse(output.player.active)
        assertEquals(1, output.stops)
    }

    @Test fun nextBlessingRestoresFullGainAndOldFadeCannotStopIt() {
        val output = Output()
        output.player.blessing(1, 0, 14500)
        output.player.replay(2000)
        output.player.tick(2500, false)
        output.player.blessing(2, 2600, 14500)
        output.player.tick(3100, false)
        assertTrue(output.player.active)
        assertEquals(1f, output.gain, 0f)
        assertEquals(2, output.starts)
        assertEquals(1, output.stops)
    }

    @Test fun pauseOrDisposalStopsAndLateTicksCannotResume() {
        val output = Output()
        output.player.blessing(1, 0, 14500)
        output.player.replay(2000)
        output.player.stop()
        output.player.stop()
        output.player.tick(2500, false)
        output.player.blessing(1, 3000, 14500)
        assertFalse(output.player.active)
        assertEquals(1, output.starts)
        assertEquals(1, output.stops)
    }

    @Test fun stalledPlaybackHeadStillStopsAtWatchdogDeadline() {
        val output = Output()
        output.player.blessing(1, 1000, 14500)
        output.player.tick(15749, false)
        assertTrue(output.player.active)
        output.player.tick(15750, false)
        assertFalse(output.player.active)
        assertEquals(1, output.stops)
    }

    @Test fun bundledMusicPreservesUserWavAndStereoFrameDuration() {
        val wav = File("src/main/res/raw/ending_bgm.wav").readBytes()
        val hash = MessageDigest.getInstance("SHA-256").digest(wav).joinToString("") { "%02x".format(it) }
        assertEquals("685cf1cc05d88a32ac7152da2ad204364cc61c7b772a2e68c8d412823df9ca7d", hash)
        assertArrayEquals(File("../endingBGM.wav").readBytes(), wav)
        val clip = PcmWaveClip.decode(wav, expectedChannels = 2)
        assertEquals(2, clip.channels)
        assertEquals(44100, clip.sampleRate)
        assertEquals(639450, clip.frameCount)
        assertEquals(14500L, clip.durationMs)
        assertArrayEquals(clip.samples, clip.attenuated(1f).samples)
    }

    @Test fun monoTearDecoderRejectsStereoAndTruncatedMusicIsRejected() {
        val wav = File("src/main/res/raw/ending_bgm.wav").readBytes()
        assertThrows(IllegalArgumentException::class.java) { PcmWaveClip.decode(wav) }
        assertThrows(IllegalArgumentException::class.java) { PcmWaveClip.decode(wav.copyOf(wav.size - 1), 2) }
    }
}
