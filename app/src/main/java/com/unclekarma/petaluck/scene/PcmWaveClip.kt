package com.unclekarma.petaluck.scene

import kotlin.math.roundToInt

/** Chunk-aware PCM16 decoder; petal audio remains mono, ending music is stereo. */
internal class PcmWaveClip private constructor(val sampleRate: Int, val samples: ByteArray, val channels: Int) {
    val frameCount: Int get() = samples.size / (2 * channels)
    val durationMs: Long get() = (frameCount * 1000L + sampleRate - 1) / sampleRate

    /** Scale signed little-endian PCM16 before the spatial/native playback path sees it. */
    fun attenuated(gain: Float): PcmWaveClip {
        require(gain.isFinite() && gain in 0f..1f) { "PCM attenuation must be in 0..1" }
        val output = ByteArray(samples.size)
        for (offset in samples.indices step 2) {
            val sample = ((samples[offset].toInt() and 255) or
                (samples[offset + 1].toInt() shl 8)).toShort().toInt()
            val scaled = (sample * gain).roundToInt()
            output[offset] = scaled.toByte()
            output[offset + 1] = (scaled shr 8).toByte()
        }
        return PcmWaveClip(sampleRate, output, channels)
    }

    companion object {
        fun decode(wav: ByteArray, expectedChannels: Int = 1): PcmWaveClip {
            require(expectedChannels in 1..2)
            fun tag(offset: Int) = String(wav, offset, 4, Charsets.US_ASCII)
            fun u16(offset: Int) = (wav[offset].toInt() and 255) or
                ((wav[offset + 1].toInt() and 255) shl 8)
            fun u32(offset: Int) = u16(offset).toLong() or (u16(offset + 2).toLong() shl 16)
            require(wav.size >= 12 && tag(0) == "RIFF" && tag(8) == "WAVE") { "Not a RIFF WAV" }
            val end = u32(4) + 8
            require(end in 12..wav.size.toLong()) { "Truncated WAV" }
            var offset = 12L
            var sampleRate: Int? = null
            var samples: ByteArray? = null
            while (offset + 8 <= end) {
                val header = offset.toInt()
                val size = u32(header + 4)
                val start = header + 8
                val next = start + size + (size and 1)
                require(next <= end) { "Truncated WAV chunk" }
                when (tag(header)) {
                    "fmt " -> {
                        require(sampleRate == null && size >= 16) { "Invalid WAV format chunk" }
                        require(u16(start) == 1 && u16(start + 2) == expectedChannels &&
                            u16(start + 12) == 2 * expectedChannels && u16(start + 14) == 16) { "Unexpected PCM16 channel format" }
                        val rate = u32(start + 4)
                        require(rate in 8000..192000 && u32(start + 8) == rate * 2 * expectedChannels) { "Invalid PCM rate" }
                        sampleRate = rate.toInt()
                    }
                    "data" -> {
                        require(samples == null && size > 0 && size % (2 * expectedChannels) == 0L) { "Invalid PCM samples" }
                        samples = wav.copyOfRange(start, (start + size).toInt())
                    }
                }
                offset = next
            }
            require(offset == end) { "Incomplete WAV chunk header" }
            return PcmWaveClip(requireNotNull(sampleRate) { "Missing WAV format" },
                requireNotNull(samples) { "Missing WAV samples" }, expectedChannels)
        }
    }
}
