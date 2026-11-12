package com.example.flowertears.p18ab52cf.domain

/** Scene-frame clock; no wall-clock jumps when the app stops producing active frames. */
class RoseEntrance {
    data class Frame(val scale: Float, val opacity: Float, val finished: Boolean)
    private var elapsed = 0f
    private var running = false

    fun start(): Frame {
        elapsed = 0f
        running = true
        return sample(0f)
    }

    fun advance(deltaSeconds: Float): Frame? {
        if (!running) return null
        if (deltaSeconds.isFinite() && deltaSeconds > 0f) {
            elapsed = (elapsed + deltaSeconds).coerceAtMost(DURATION_SECONDS)
        }
        return sample(elapsed).also { if (it.finished) running = false }
    }

    fun clear() { elapsed = 0f; running = false }

    companion object {
        const val ROOT_OFFSET_Y = -.10f
        const val DURATION_SECONDS = .8f

        fun sample(seconds: Float): Frame {
            val t = (seconds / DURATION_SECONDS).coerceIn(0f, 1f)
            val eased = t*t*(3f - 2f*t)
            return Frame(eased, eased, t >= 1f)
        }
    }
}
