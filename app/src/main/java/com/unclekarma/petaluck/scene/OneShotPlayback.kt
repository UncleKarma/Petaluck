package com.unclekarma.petaluck.scene

/** Owns one short cue; old completion callbacks must never stop a newer pluck. */
internal class OneShotPlayback(
    private val frameCount: Int,
    private val startOutput: (Long) -> Unit,
    private val stopOutput: () -> Unit,
) {
    private var generation = 0L
    private var active: Long? = null

    fun play(): Long {
        stop()
        val token = ++generation
        active = token
        try { startOutput(token) }
        catch (e: Exception) {
            try { stop() } catch (cleanup: Exception) { e.addSuppressed(cleanup) }
            throw e
        }
        return token
    }

    fun completed(token: Long, playedFrames: Int): Boolean =
        playedFrames >= frameCount && finish(token)

    /** A bounded fallback for lost native completion notifications. */
    fun timeout(token: Long): Boolean = finish(token)

    private fun finish(token: Long): Boolean {
        if (active != token) return false
        stop()
        return true
    }

    fun stop() {
        if (active == null) return
        active = null
        stopOutput()
    }
}
