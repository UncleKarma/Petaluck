package com.example.flowertears.p18ab52cf.scene

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.example.flowertears.p18ab52cf.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Non-positional stereo music, independent of the current flower and its spatial tear track. */
internal class EndingMusic(
    context: Context,
    onStarted: (Long) -> Unit = {},
    onStopped: () -> Unit = {},
    onFinished: () -> Unit = onStopped,
) : AutoCloseable {
    private val resources = context.applicationContext.resources
    private val handler = Handler(Looper.getMainLooper())
    private var track: AudioTrack? = null
    private var frameCount = 0
    private var durationMs = 0L
    private val playback = EndingMusicPlayback(
        startOutput = {
            val output = checkNotNull(track)
            check(output.reloadStaticData() == AudioTrack.SUCCESS) { "Cannot rewind ending music" }
            output.play()
        },
        setGain = { gain ->
            check(checkNotNull(track).setVolume(gain) == AudioTrack.SUCCESS) { "Cannot set ending music gain" }
        },
        stopOutput = { track?.stop() },
        onStarted = onStarted,
        onStopped = onStopped,
        onFinished = onFinished,
    )
    private val pulse = object : Runnable {
        override fun run() {
            safely("update") {
                playback.tick(SystemClock.uptimeMillis(), (track?.playbackHeadPosition ?: 0) >= frameCount)
                if (playback.active) handler.postDelayed(this, 16L)
                else Log.i(TAG, "EndingMusicStopped")
            }
        }
    }

    suspend fun prepare() {
        if (track != null) return
        val clip = withContext(Dispatchers.IO) {
            resources.openRawResource(R.raw.ending_bgm).use {
                PcmWaveClip.decode(it.readBytes(), expectedChannels = 2)
            }
        }
        val output = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(clip.sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(clip.samples.size).build()
        try {
            check(output.write(clip.samples, 0, clip.samples.size) == clip.samples.size) { "Cannot preload ending music" }
            check(output.state == AudioTrack.STATE_INITIALIZED) { "Cannot initialize ending music" }
            // Preserve the user's PCM at unity; no audio-focus request, media session or system volume change.
            frameCount = clip.frameCount
            durationMs = clip.durationMs
            track = output
            Log.i(TAG, "EndingMusicReady frames=$frameCount durationMs=$durationMs channels=2")
        } catch (e: Exception) {
            output.release()
            throw e
        }
    }

    fun blessing(roundId: Long) {
        if (track == null) { Log.w(TAG, "EndingMusicNotReady round=$roundId"); return }
        safely("play") {
            playback.blessing(roundId, SystemClock.uptimeMillis(), durationMs)
            handler.removeCallbacks(pulse)
            if (playback.active) handler.postDelayed(pulse, 16L)
            Log.i(TAG, "EndingMusicBlessing round=$roundId")
        }
    }

    fun replay() {
        playback.replay(SystemClock.uptimeMillis())
        if (playback.active) Log.i(TAG, "EndingMusicFadeOut durationMs=${EndingMusicPlayback.FADE_MS}")
    }

    fun stop() {
        handler.removeCallbacks(pulse)
        try { playback.stop() }
        catch (e: Exception) { Log.e(TAG, "Ending music stop failed", e) }
    }

    private inline fun safely(action: String, block: () -> Unit) {
        try { block() }
        catch (e: Exception) {
            stop()
            Log.e(TAG, "Ending music $action failed; flower remains playable", e)
        }
    }

    override fun close() {
        stop()
        val previous = track
        track = null
        try { previous?.release() }
        catch (e: Exception) { Log.e(TAG, "Ending music release failed", e) }
    }

    companion object { private const val TAG = "FlowerTears" }
}
