package com.unclekarma.petaluck

import com.unclekarma.petaluck.scene.PcmWaveClip
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class PcmWaveClipTest {
    private fun source() = File("src/main/res/raw/petal_drop.wav").readBytes()
    private fun ByteArray.put32(offset: Int, value: Int) {
        ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).putInt(offset, value)
    }
    private fun ByteArray.dataChunkHeader(): Int {
        val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 12
        while (offset + 8 <= size) {
            if (String(this, offset, 4, Charsets.US_ASCII) == "data") return offset
            val length = buffer.getInt(offset + 4)
            offset += 8 + length + (length and 1)
        }
        error("Missing data chunk in test fixture")
    }

    @Test fun originalUserClipIsDecodedWithoutChangingAnySample() {
        val wav = source()
        val clip = PcmWaveClip.decode(wav)
        assertEquals(48000, clip.sampleRate)
        assertEquals(3600, clip.frameCount)
        assertEquals(75L, clip.durationMs)
        val dataHeader = wav.dataChunkHeader()
        val length = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN).getInt(dataHeader + 4)
        val expected = wav.copyOfRange(dataHeader + 8, dataHeader + 8 + length)
        assertArrayEquals(expected, clip.samples)
        assertArrayEquals(File("../petalDrop.wav").readBytes(), wav)
    }

    @Test fun ancillaryChunksAndOddBytePaddingAreNotPlayedAsSamples() {
        val wav = source()
        val chunk = byteArrayOf(74, 85, 78, 75, 3, 0, 0, 0, 1, 2, 3, 0) // JUNK + padding
        val extended = wav.copyOfRange(0, 12) + chunk + wav.copyOfRange(12, wav.size)
        extended.put32(4, extended.size - 8)
        assertArrayEquals(PcmWaveClip.decode(wav).samples, PcmWaveClip.decode(extended).samples)
    }

    @Test fun truncatedOrOverflowingChunksAreRejectedBeforeTrackCreation() {
        val wav = source()
        assertThrows(IllegalArgumentException::class.java) { PcmWaveClip.decode(wav.copyOf(43)) }
        wav.put32(16, -1)
        assertThrows(IllegalArgumentException::class.java) { PcmWaveClip.decode(wav) }
    }

    @Test fun unsupportedFormatCannotBeAccidentallyPlayedAsMonoPcm() {
        val wav = source().apply { this[22] = 2 } // Stereo fmt with mono samples.
        assertThrows(IllegalArgumentException::class.java) { PcmWaveClip.decode(wav) }
        val compressed = source().apply { this[20] = 3 } // IEEE float is not PCM16.
        assertThrows(IllegalArgumentException::class.java) { PcmWaveClip.decode(compressed) }
    }

    @Test fun malformedPcmLengthOrRateIsRejected() {
        val odd = source().apply { put32(dataChunkHeader() + 4, 7199) }
        assertThrows(IllegalArgumentException::class.java) { PcmWaveClip.decode(odd) }
        val rate = source().apply { put32(24, 0) }
        assertThrows(IllegalArgumentException::class.java) { PcmWaveClip.decode(rate) }
    }
}
