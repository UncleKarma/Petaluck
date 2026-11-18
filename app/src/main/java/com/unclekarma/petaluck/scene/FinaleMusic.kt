package com.unclekarma.petaluck.scene

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.unclekarma.petaluck.R

/** Main-thread playback for one colorful finale, shared by its Stage and the volume's exit button. */
internal class FinaleMusic(private val context: Context) : AutoCloseable {
    private var player: MediaPlayer? = null
    private var closed = false

    val isPlaying: Boolean get() = player?.isPlaying == true

    fun play() {
        if (closed) return
        try {
            if (isPlaying) return
            val output = player ?: checkNotNull(MediaPlayer.create(context, R.raw.petaluck_game_ending_bgm,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                AudioManager.AUDIO_SESSION_ID_GENERATE)).also {
                player = it
                it.isLooping = true
                it.setOnErrorListener { failed, what, extra ->
                    Log.e(TAG, "Finale music decoder error what=$what extra=$extra")
                    if (failed === player) releasePlayer()
                    true
                }
            }
            output.start()
            Log.i(TAG, "FinaleMusicStarted durationMs=${output.duration} looping=${output.isLooping}")
        } catch (e: Exception) {
            releasePlayer()
            Log.e(TAG, "Could not play finale music", e)
        }
    }

    fun pause() {
        try {
            if (isPlaying) player?.pause()
        } catch (e: IllegalStateException) {
            releasePlayer()
            Log.e(TAG, "Could not pause finale music", e)
        }
    }

    fun releasePlayer() {
        val output = player ?: return
        player = null
        output.release()
        Log.i(TAG, "FinaleMusicReleased")
    }

    override fun close() {
        closed = true
        releasePlayer()
    }

    private companion object { const val TAG = "Petaluck" }
}
