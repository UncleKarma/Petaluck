package com.example.flowertears.p18ab52cf.domain

/** Captures input in its original source space; the adapter maps displacement to scene meters. */
class PinchDragTracker {
    enum class Kind { Direct, Remote, Other }
    enum class PositionSource { LocalPointer, GlobalDevicePose }
    data class Sample(val pointerId: Long, val down: Boolean, val up: Boolean, val pressed: Boolean,
        val kind: Kind, val targetPetal: Int?)
    sealed interface Step {
        data class Started(val pointerId: Long, val petal: Int, val source: PositionSource) : Step
        data class Anchored(val pointerId: Long, val downPosition: Vec, val position: Vec) : Step
        data class Moved(val pointerId: Long, val delta: Vec, val frameDelta: Vec = Vec()) : Step
        data class Unavailable(val pointerId: Long) : Step
        data class Released(val pointerId: Long, val cancelled: Boolean) : Step
    }
    private data class Capture(val pointerId: Long, val petal: Int, val source: PositionSource,
        val origin: Vec, var previous: Vec?, var accumulated: Vec = Vec())
    private val pending = mutableMapOf<Long, Int>()
    private var capture: Capture? = null
    val positionSource: PositionSource? get() = capture?.source

    fun update(sample: Sample, position: (PositionSource) -> Vec?,
        grab: (petal: Int, pointerId: Long) -> Boolean,
        displacement: (source: PositionSource, origin: Vec, current: Vec) -> Vec = { _, origin, current -> current - origin },
    ): Step? {
        val held = capture
        if (held?.pointerId == sample.pointerId) {
            if (sample.up || !sample.pressed) {
                clear()
                return Step.Released(sample.pointerId, cancelled = !sample.up)
            }
            // Repeated down metadata is not part of the remote movement stream.
            if (held.source == PositionSource.GlobalDevicePose && sample.down) return null
            // Keep both owner and coordinate source after the pointer leaves the collider,
            // after detach, or when the latest metadata changes/misses kind.
            val current = position(held.source)?.takeIf { it.finite() }
                ?: return Step.Unavailable(sample.pointerId)
            if (held.source == PositionSource.GlobalDevicePose) {
                val previous = held.previous
                if (previous == null) {
                    // Native down and move poses can have a discontinuity at gesture activation.
                    // Anchor once on the first valid move; never infer motion from the down pose.
                    held.previous = current
                    return Step.Anchored(sample.pointerId, held.origin, current)
                }
                // Convert only adjacent samples. A scale change must never remap past movement.
                val frameDelta = displacement(held.source, previous, current).takeIf { it.finite() }
                    ?: return Step.Unavailable(sample.pointerId)
                val total = (held.accumulated + frameDelta).takeIf { it.finite() }
                    ?: return Step.Unavailable(sample.pointerId)
                held.previous = current
                held.accumulated = total
                return Step.Moved(sample.pointerId, total, frameDelta)
            }
            val delta = displacement(held.source, held.origin, current).takeIf { it.finite() }
                ?: return Step.Unavailable(sample.pointerId)
            return Step.Moved(sample.pointerId, delta)
        }
        if (sample.up || !sample.pressed) { pending.remove(sample.pointerId); return null }
        if (sample.down) {
            if (held == null && sample.targetPetal != null) pending[sample.pointerId] = sample.targetPetal
            else pending.remove(sample.pointerId)
        }
        if (held != null) return null
        val petal = pending[sample.pointerId] ?: return null
        val source = when (sample.kind) {
            Kind.Direct -> PositionSource.LocalPointer
            Kind.Remote -> PositionSource.GlobalDevicePose
            Kind.Other -> return null // A poke alone is never a pluck; a later pinch can begin.
        }
        val origin = position(source)?.takeIf { it.finite() } ?: return null
        if (!grab(petal, sample.pointerId)) { pending.remove(sample.pointerId); return null }
        // A pending target may first acquire a valid pinch on a move, which is already an anchor.
        capture = Capture(sample.pointerId, petal, source, origin,
            previous = if (source == PositionSource.GlobalDevicePose && sample.down) null else origin)
        // Other fingers already down must make a fresh pinch after this gesture finishes.
        pending.clear()
        return Step.Started(sample.pointerId, petal, source)
    }

    fun clear() { capture = null; pending.clear() }
    private fun Vec.finite() = x.isFinite() && y.isFinite() && z.isFinite()
}
