package com.unclekarma.petaluck.domain

/** Erase while masked, then disable the empty surface so it cannot intercept the next flower. */
class CompletionBufferGate {
    data class Request(val serial: Long, val revealRound: Long?)

    var request: Request? = null; private set
    var presentedRound: Long? = null; private set
    var enabled = false; private set
    private var completingRound: Long? = null
    private var serial = 0L

    fun update(roundId: Long, completing: Boolean) {
        val next = roundId.takeIf { completing }
        if (next == completingRound) return
        completingRound = next
        presentedRound = null
        if (next != null || enabled) {
            // Null means erase before disabling. Loading round IDs may change while it drains.
            request = Request(++serial, next)
            enabled = true
        }
    }

    fun committed(frame: Request): Boolean {
        if (request != frame) return false
        request = null
        presentedRound = frame.revealRound
        enabled = frame.revealRound != null
        return true
    }

    fun reset() {
        ++serial // A callback from the previous owner must never unlock its replacement.
        request = null
        presentedRound = null
        completingRound = null
        enabled = false
    }
}
