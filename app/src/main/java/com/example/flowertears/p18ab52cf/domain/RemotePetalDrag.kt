package com.example.flowertears.p18ab52cf.domain

import kotlin.math.sqrt

/** One ordered stream of scene-meter displacements, from pinch down through release. */
class RemotePetalDrag {
    var active = false; private set
    var displacement = Vec(); private set
    private val pending = ArrayDeque<Vec>()
    private val held = IncrementalPetalDrag()

    fun start() { clear(); active = true }

    fun moveBy(delta: Vec) {
        if (active && delta.x.isFinite() && delta.y.isFinite() && delta.z.isFinite()) pending.addLast(delta)
    }

    /**
     * Drain on a scene frame, and once before release. Do not sum samples before testing the
     * threshold: a hand can cross it and return within one frame.
     * At the crossing, pull must preserve/reparent the full pose and return its root position.
     */
    fun advance(pull: (displacement: Vec, crossing: Boolean) -> Vec?): Vec? {
        if (!active) return null
        while (pending.isNotEmpty()) {
            val delta = pending.removeFirst()
            if (held.position != null) {
                held.moveBy(delta)
                continue
            }
            val next = displacement + delta
            if (PullRules.distance(next) < PullRules.THRESHOLD) {
                displacement = next
                pull(next, false)
                continue
            }
            val fraction = crossingFraction(displacement, delta)
            val beforeTear = delta * fraction
            displacement += beforeTear
            // The callback owns native transforms and the session's idempotent detach.
            held.detach(checkNotNull(pull(displacement, true)) { "Remote detach did not preserve a pose" })
            // Only the unconsumed portion follows the detached petal. No cap and no dt factor.
            held.moveBy(delta - beforeTear)
        }
        return held.advance()
    }

    fun clear() {
        active = false
        displacement = Vec()
        pending.clear()
        held.clear()
    }

    private fun crossingFraction(start: Vec, delta: Vec): Float {
        fun dot(a: Vec, b: Vec) = a.x.toDouble()*b.x + a.y.toDouble()*b.y + a.z.toDouble()*b.z
        // Solve |start + t * delta| = threshold. Start is inside the sphere; take its exit.
        val a = dot(delta, delta)
        val b = dot(start, delta)
        val radius = PullRules.THRESHOLD.toDouble()
        val c = dot(start, start) - radius*radius
        val root = sqrt((b*b - a*c).coerceAtLeast(0.0))
        // Avoid cancellation for a small remaining distance while moving outward.
        val t = if (b >= 0.0) -c / (root + b) else (root - b) / a
        return t.coerceIn(0.0, 1.0).toFloat()
    }
}
