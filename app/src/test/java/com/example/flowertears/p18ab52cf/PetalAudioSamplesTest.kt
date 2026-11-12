package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.scene.PcmWaveClip
import com.example.flowertears.p18ab52cf.scene.PetalAudioSamples
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt
import org.junit.Assert.*
import org.junit.Test

class PetalAudioSamplesTest {
    private fun source() = File("src/main/res/raw/petal_drop.wav").readBytes()
    private fun samples(bytes: ByteArray): List<Int> {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return List(bytes.size / 2) { buffer.short.toInt() }
    }
    private fun rms(values: List<Int>) = sqrt(values.sumOf { it.toDouble() * it } / values.size)
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    @Test fun actualPlaybackBufferHasHalfThePreviousGainBeforeSpatialProcessing() {
        val wav = source()
        val unchanged = wav.copyOf()
        val original = PcmWaveClip.decode(wav)
        // This is the exact factory called immediately before PetalSpatialAudio writes its buffer.
        val playback = PetalAudioSamples.fromWav(wav)
        val before = samples(original.samples)
        val after = samples(playback.samples)
        assertEquals(original.sampleRate, playback.sampleRate)
        assertEquals(original.frameCount, playback.frameCount)
        assertEquals(original.durationMs, playback.durationMs)
        assertArrayEquals(unchanged, wav)
        for (i in before.indices) assertTrue(abs(after[i] - before[i] * .025) <= .501)
        val peakBefore = before.maxOf { abs(it) }
        val peakAfter = after.maxOf { abs(it) }
        val rmsBefore = rms(before)
        val rmsAfter = rms(after)
        val reductionDb = 20 * log10(rmsAfter / rmsBefore)
        assertEquals(.025, rmsAfter / rmsBefore, .00001)
        assertEquals(20 * log10(.025), reductionDb, .001)
        val previous = samples(original.attenuated(.05f).samples)
        assertEquals(.5, rmsAfter / rms(previous), .0001)
        assertTrue(peakAfter < peakBefore * .026)
        val dir = File("build/reports/petal-audio-input").apply { mkdirs() }
        File(dir, "pcm-gain-v3.pcm").writeBytes(playback.samples)
        File(dir, "measurement.json").writeText("""{
          "sampleRate":${playback.sampleRate},"frames":${playback.frameCount},
          "durationMs":${playback.durationMs},"pcmGain":${PetalAudioSamples.PCM_GAIN},
          "sourcePeak":$peakBefore,"playbackPeak":$peakAfter,
          "sourceRms":$rmsBefore,"playbackRms":$rmsAfter,"rmsChangeDb":$reductionDb,
          "sourcePcmSha256":"${hash(original.samples)}","playbackPcmSha256":"${hash(playback.samples)}"
        }""")
    }

    @Test fun negativeAndPositivePcm16BoundariesKeepTheirSignAndLittleEndianEncoding() {
        val input = listOf(-32768, -32767, -10000, -1000, -10, -1, 0, 1, 10, 1000, 10000, 32767)
        val pcm = ByteBuffer.allocate(input.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            .apply { input.forEach { putShort(it.toShort()) } }.array()
        // Build a standalone PCM fixture; user WAVs may contain metadata before their data chunk.
        val wav = ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII)); putInt(36 + pcm.size)
            put("WAVEfmt ".toByteArray(Charsets.US_ASCII)); putInt(16)
            putShort(1); putShort(1); putInt(48000); putInt(96000)
            putShort(2); putShort(16)
            put("data".toByteArray(Charsets.US_ASCII)); putInt(pcm.size); put(pcm)
        }.array()
        val result = samples(PetalAudioSamples.fromWav(wav).samples)
        assertEquals(listOf(-819, -819, -250, -25, 0, 0, 0, 0, 0, 25, 250, 819), result)
    }

    @Test fun zeroIsSilenceAndUnityKeepsTheOriginalWithoutMutatingItsBuffer() {
        val original = PcmWaveClip.decode(source())
        val copy = original.samples.copyOf()
        assertTrue(original.attenuated(0f).samples.all { it == 0.toByte() })
        val unity = original.attenuated(1f)
        assertArrayEquals(copy, unity.samples)
        unity.samples[0] = (unity.samples[0].toInt() xor 127).toByte()
        assertArrayEquals(copy, original.samples)
    }

    @Test fun invalidOrAmplifyingGainsAreRejected() {
        val clip = PcmWaveClip.decode(source())
        for (gain in listOf(-.1f, 1.1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { clip.attenuated(gain) }
        }
    }

    @Test fun preparingAnotherRoundDoesNotCompoundAttenuationOrChangeTheSource() {
        val wav = source()
        val sourceHash = hash(wav)
        val expected = PetalAudioSamples.fromWav(wav).samples
        repeat(20) { assertArrayEquals(expected, PetalAudioSamples.fromWav(wav).samples) }
        assertEquals(sourceHash, hash(wav))
    }
}
