package com.example.flowertears.p18ab52cf.scene

/** Main-thread, scene-owned playback: round teardown must not cut off a replay fade. */
internal class EndingMusicPlayback(
    private val startOutput: () -> Unit,
    private val setGain: (Float) -> Unit,
    private val stopOutput: () -> Unit,
) {
    var active = false; private set
    private var lastRound: Long? = null
    private var fadeStartMs: Long? = null
    private var deadlineMs = 0L

    fun blessing(roundId: Long, nowMs: Long, durationMs: Long) {
        if (lastRound == roundId) return
        lastRound = roundId
        stop()
        active = true // Allows failure cleanup even if the platform start throws.
        deadlineMs = nowMs + durationMs + 250L
        setGain(1f)
        startOutput()
    }

    fun replay(nowMs: Long) {
        if (active && fadeStartMs == null) fadeStartMs = nowMs
    }

    fun tick(nowMs: Long, ended: Boolean) {
        if (!active) return
        if (ended || nowMs >= deadlineMs) { stop(); return }
        fadeStartMs?.let { start ->
            val gain = (1f - (nowMs - start).coerceAtLeast(0L) / FADE_MS.toFloat()).coerceIn(0f, 1f)
            setGain(gain)
            if (gain == 0f) stop()
        }
    }

    fun stop() {
        val wasActive = active
        active = false
        fadeStartMs = null
        if (wasActive) stopOutput()
    }

    companion object { const val FADE_MS = 1000L }
}
