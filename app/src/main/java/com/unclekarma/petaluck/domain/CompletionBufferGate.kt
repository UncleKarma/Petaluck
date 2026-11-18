package com.unclekarma.petaluck.domain

/** Erase while masked, then disable the empty surface so it cannot intercept the next flower. */
class CompletionBufferGate {
    enum class Draw { Blank, Blessing }
    data class Request(val serial: Long, val revealRound: Long?, val draw: Draw)
    var fadeProgress = 0f; private set

    var request: Request? = null; private set
    var presentedRound: Long? = null; private set
    var enabled = false; private set
    private var completingRound: Long? = null
    private var serial = 0L

    fun update(roundId: Long, completing: Boolean) {
        val next = roundId.takeIf { completing }
        if (next == completingRound) return
        completingRound = next
        fadeProgress = 0f
        presentedRound = null
        if (next != null || enabled) {
            // Null means erase before disabling. Loading round IDs may change while it drains.
            request = Request(++serial, next, Draw.Blank)
            enabled = true
        }
    }

    fun committed(frame: Request): Boolean {
        if (request != frame) return false
        if (frame.revealRound != null && frame.draw == Draw.Blank) {
            // Submit the new wish while the native owner is still fully masked.
            request = Request(++serial, frame.revealRound, Draw.Blessing)
            return true
        }
        request = null
        presentedRound = frame.revealRound
        enabled = frame.revealRound != null
        return true
    }

    fun advanceFade(deltaSeconds: Float): Boolean {
        if (presentedRound == null || fadeProgress >= 1f || deltaSeconds <= 0f) return false
        fadeProgress = (fadeProgress + deltaSeconds / .6f).coerceAtMost(1f)
        return true
    }

    fun reset() {
        fadeProgress = 0f
        ++serial // A callback from the previous owner must never unlock its replacement.
        request = null
        presentedRound = null
        completingRound = null
        enabled = false
    }
}
