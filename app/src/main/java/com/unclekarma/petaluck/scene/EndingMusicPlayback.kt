package com.unclekarma.petaluck.scene

/** Main-thread, scene-owned playback: round teardown must not cut off a replay fade. */
internal class EndingMusicPlayback(
    private val startOutput: () -> Unit,
    private val setGain: (Float) -> Unit,
    private val stopOutput: () -> Unit,
    private val onStarted: (Long) -> Unit = {},
    private val onStopped: () -> Unit = {},
    private val onFinished: () -> Unit = onStopped,
) {
    var active = false; private set
    private var lastRound: Long? = null
    private var fadeStartMs: Long? = null
    private var deadlineMs = 0L
    private var visualActive = false

    fun blessing(roundId: Long, nowMs: Long, durationMs: Long) {
        if (lastRound == roundId) return
        lastRound = roundId
        stop()
        active = true // Allows failure cleanup even if the platform start throws.
        deadlineMs = nowMs + durationMs + 250L
        setGain(1f)
        startOutput()
        visualActive = true
        onStarted(roundId)
    }

    fun replay(nowMs: Long) {
        if (active && fadeStartMs == null) {
            fadeStartMs = nowMs
            stopVisual() // End before the music fade; its later completion must not stop a new preload.
        }
    }

    fun tick(nowMs: Long, ended: Boolean) {
        if (!active) return
        if (ended || nowMs >= deadlineMs) {
            // A natural end may leave an independently timed visual tail alive.
            stop(naturalEnd = fadeStartMs == null)
            return
        }
        fadeStartMs?.let { start ->
            val gain = (1f - (nowMs - start).coerceAtLeast(0L) / FADE_MS.toFloat()).coerceIn(0f, 1f)
            setGain(gain)
            if (gain == 0f) stop()
        }
    }

    fun stop(naturalEnd: Boolean = false) {
        val wasActive = active
        active = false
        fadeStartMs = null
        if (wasActive) {
            try { stopOutput() } finally { stopVisual(naturalEnd) }
        }
    }

    private fun stopVisual(naturalEnd: Boolean = false) {
        if (!visualActive) return
        visualActive = false
        if (naturalEnd) onFinished() else onStopped()
    }

    companion object { const val FADE_MS = 1000L }
}
