package com.unclekarma.petaluck.domain

/** Remote-only held motion: add each hand delta once to the petal's current root-relative pose. */
class IncrementalPetalDrag {
    var position: Vec? = null; private set
    private var pendingDelta = Vec()

    /** The threshold-crossing event establishes the pose; its movement is already included. */
    fun detach(visiblePosition: Vec) {
        position = visiblePosition
        pendingDelta = Vec()
    }

    fun moveBy(frameDelta: Vec) {
        if (position != null) pendingDelta += frameDelta
    }

    /** Scene frames drain all input samples since the previous frame, without multiplying by dt. */
    fun advance(): Vec? {
        val current = position ?: return null
        val next = current + pendingDelta
        pendingDelta = Vec()
        position = next
        return next
    }

    fun clear() { position = null; pendingDelta = Vec() }
}
