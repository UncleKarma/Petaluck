package com.unclekarma.petaluck.scene

/** One attenuation authority for the actual PCM bytes written to the spatial AudioTrack. */
internal object PetalAudioSamples {
    const val PCM_GAIN = .025f
    const val REVISION = "pcm-gain-v3"

    fun fromWav(wav: ByteArray): PcmWaveClip = PcmWaveClip.decode(wav).attenuated(PCM_GAIN)
}
