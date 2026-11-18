package com.unclekarma.petaluck.scene

/** Scene-owned deadlines; stale callbacks must never affect another round's native instance. */
internal class PetalRainSchedule(
    private val schedule: (Long, () -> Unit) -> Unit,
    private val cancelScheduled: () -> Unit,
    private val onDrain: (Long) -> Unit,
    private val onFinished: (Long) -> Unit,
) {
    private var generation = 0L
    private var round: Long? = null
    private var draining = false

    fun start(roundId: Long, tailDurationMs: Long) {
        if (round == roundId) return
        cancel()
        round = roundId
        val token = generation
        schedule(EMIT_MS) {
            if (generation == token && round == roundId && !draining) {
                draining = true
                onDrain(roundId)
                if (generation == token) schedule(tailDurationMs) {
                    if (generation == token && round == roundId) {
                        cancel()
                        onFinished(roundId)
                    }
                }
            }
        }
    }

    fun cancel() {
        generation++
        round = null
        draining = false
        cancelScheduled()
    }

    companion object {
        const val EMIT_MS = 10_000L
    }
}
