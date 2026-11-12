package com.example.flowertears.p18ab52cf.scene

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.flowertears.p18ab52cf.R
import com.pico.spatial.audio.SpatialAudioTrackExtension
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Vector3

/** One positional sound-effect track, prepared after flower readiness on real scene frames. */
class PetalSpatialAudio(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var clip: PcmWaveClip? = null
    private class Voice(
        val emitter: Entity,
        val extension: SpatialAudioTrackExtension,
        val track: AudioTrack,
    ) {
        lateinit var playback: OneShotPlayback
        var timeout: Runnable? = null
    }
    private var voice: Voice? = null

    suspend fun prepare(parent: Entity, awaitSceneFrame: suspend () -> Unit) {
        check(voice == null) { "Clear previous round audio before preparing another" }
        // Audio remains optional, after Ready, and outside flower assembly.
        awaitSceneFrame()
        val audio = clip ?: appContext.resources.openRawResource(R.raw.petal_drop).use {
            PetalAudioSamples.fromWav(it.readBytes())
        }.also { clip = it }
        val emitter = Entity()
        var extension: SpatialAudioTrackExtension? = null
        var track: AudioTrack? = null
        try {
            emitter.setName("PetalTearAudio")
            emitter.components.set(TransformComponent())
            check(parent.addChild(emitter)) { "Cannot attach petal sound emitter" }
            awaitSceneFrame()
            val builder = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(audio.sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(audio.samples.size)
            extension = SpatialAudioTrackExtension()
            // Public SDK 6.2.8 bridge: retain full positional audio without a media player/focus owner.
            extension.spatialAudioTrackExtensionConfig(
                mode = SpatialAudioTrackExtension.SpatialAudioMode.OBJECT,
                isAmbisonic = false,
                builder = builder,
            )
            extension.attachToEntityWithBuilder(emitter, builder)
            track = builder.build()
            check(track.write(audio.samples, 0, audio.samples.size) == audio.samples.size) {
                "Cannot preload petal PCM samples"
            }
            check(track.state == AudioTrack.STATE_INITIALIZED) { "Cannot initialize petal audio track" }
            // Attenuation is already in the samples; unity prevents applying the gain twice.
            check(track.setVolume(1f) == AudioTrack.SUCCESS) { "Cannot set petal track unity gain" }
            val current = Voice(emitter, extension, track)
            current.playback = OneShotPlayback(
                frameCount = audio.frameCount,
                startOutput = { token -> startTrack(current, audio, token) },
                stopOutput = { stopTrack(current) },
            )
            voice = current
            Log.i(TAG, "PetalAudioReady revision=${PetalAudioSamples.REVISION} tracks=1 spatialMode=OBJECT usage=SONIFICATION pcmGain=${PetalAudioSamples.PCM_GAIN} trackGain=1 frames=${audio.frameCount}")
        } catch (e: Exception) {
            release(emitter, extension, track)
            throw e
        }
    }

    /** Position is in the RoseAnchor parent's meters, sampled at the tear before stem rebound. */
    fun play(petalId: Int, position: Vector3) {
        val current = voice ?: run {
            // Do not queue a tear and emit it later from a stale position.
            Log.w(TAG, "Petal sound not ready at tear petal=$petalId")
            return
        }
        try {
            check(current.emitter.valid) { "Invalid petal audio emitter" }
            current.playback.stop()
            // The source stays at the tear instead of following the held/falling petal.
            current.emitter.components[TransformComponent::class.java]!!.setPosition(position)
            current.playback.play()
            Log.i(TAG, "PetalTearAudio petal=$petalId anchorPosition=$position")
        } catch (e: Exception) {
            cleanup("stop failed cue") { current.playback.stop() }
            // Feedback failure must not undo a successful pluck or replay it later.
            Log.e(TAG, "Petal sound failed petal=$petalId", e)
        }
    }

    private fun startTrack(current: Voice, audio: PcmWaveClip, token: Long) {
        val track = current.track
        check(track.reloadStaticData() == AudioTrack.SUCCESS) { "Cannot rewind petal sound" }
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack) {
                if (voice !== current) return
                cleanup("complete cue") {
                    // A queued marker from an earlier pluck cannot end a freshly rewound cue.
                    if (current.playback.completed(token, track.playbackHeadPosition)) {
                        Log.i(TAG, "PetalAudioStopped reason=completed")
                    }
                }
            }
            override fun onPeriodicNotification(track: AudioTrack) = Unit
        }, handler)
        check(track.setNotificationMarkerPosition(audio.frameCount) == AudioTrack.SUCCESS) {
            "Cannot set petal sound completion marker"
        }
        // No audio-focus request, media session, global mixer, or system volume change.
        track.play()
        val timeout = Runnable {
            if (voice === current) cleanup("completion timeout") {
                if (current.playback.timeout(token)) Log.w(TAG, "PetalAudioStopped reason=completion-timeout")
            }
        }
        current.timeout = timeout
        handler.postDelayed(timeout, audio.durationMs + COMPLETION_GRACE_MS)
    }

    private fun stopTrack(current: Voice) {
        current.timeout?.let { handler.removeCallbacks(it) }
        current.timeout = null
        cleanup("remove completion listener") { current.track.setPlaybackPositionUpdateListener(null) }
        cleanup("clear completion marker") { current.track.setNotificationMarkerPosition(0) }
        // MODE_STATIC otherwise remains PLAYSTATE_PLAYING after reaching the last sample.
        cleanup("stop track") { current.track.stop() }
    }

    fun stop() {
        voice?.let { current -> cleanup("stop cue") { current.playback.stop() } }
    }

    fun clearRound() {
        val previous = voice
        voice = null
        previous?.let {
            cleanup("stop cue") { it.playback.stop() }
            release(it.emitter, it.extension, it.track)
        }
    }

    private fun release(emitter: Entity, extension: SpatialAudioTrackExtension?, track: AudioTrack?) {
        cleanup("release track") { track?.release() }
        cleanup("release spatial binding") { extension?.release() }
        cleanup("destroy emitter") { if (emitter.valid) emitter.destroy() }
    }

    private inline fun cleanup(action: String, block: () -> Unit) {
        try { block() }
        catch (e: Exception) { Log.e(TAG, "Petal audio cleanup failed: $action", e) }
    }

    override fun close() {
        clearRound()
        clip = null
    }

    companion object {
        private const val TAG = "FlowerTears"
        private const val COMPLETION_GRACE_MS = 250L
    }
}
