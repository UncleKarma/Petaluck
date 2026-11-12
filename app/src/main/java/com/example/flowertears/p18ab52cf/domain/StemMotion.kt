package com.example.flowertears.p18ab52cf.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/** Shared bend and return for every pinch coordinate source. Bend units are fractions of 8 degrees. */
class StemMotion {
    private data class Return(val start: Vec, val rebound: Boolean, var elapsed: Float = 0f)

    private var bend = Vec()
    private var grabBend = Vec()
    private var returning: Return? = null

    fun grab() {
        // A new pinch can interrupt a previous return without snapping the crown to rest.
        returning = null
        grabBend = bend
    }

    fun pull(delta: Vec, strength: Float): Vec {
        val lateral = Vec(delta.x, 0f, delta.z)
        val length = lateral.length()
        val target = if (length < .001f) Vec() else lateral * (strength / length)
        bend = grabBend * (1f - strength) + target
        return bend
    }

    fun release(rebound: Boolean) {
        returning = if (bend.length() == 0f) null else Return(bend, rebound)
    }

    /** Returns a pose only while settling, including the exact rest pose on the final frame. */
    fun advance(dt: Float): Vec? {
        val motion = returning ?: return null
        motion.elapsed += dt
        val t = motion.elapsed
        val duration = if (motion.rebound) REBOUND_TIME else CANCEL_TIME
        if (t >= duration) return reset()

        val scale = if (motion.rebound) {
            // Start at the release bend with zero velocity. Fade the small last lobe to zero.
            val phase = ANGULAR_FREQUENCY * t
            val tail = ((t - .51f) / (REBOUND_TIME - .51f)).coerceIn(0f, 1f)
            val envelope = exp(-DAMPING * t) * (1f - tail * tail * (3f - 2f * tail))
            envelope * (cos(phase) + DAMPING / ANGULAR_FREQUENCY * sin(phase))
        } else {
            1f - t / duration
        }
        bend = motion.start * scale
        return bend
    }

    fun reset(): Vec {
        returning = null
        grabBend = Vec()
        bend = Vec()
        return bend
    }

    companion object {
        const val CANCEL_TIME = .22f
        private const val REBOUND_TIME = .64f
        private const val DAMPING = 8.6f
        private val ANGULAR_FREQUENCY = (2 * PI / .44).toFloat()
    }
}
