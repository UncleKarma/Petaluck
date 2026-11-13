package com.example.flowertears.p18ab52cf.domain

import com.pico.spatial.core.math.Quat

/** Device and petal quaternions must both use RoseAnchor's axes (no entrance rotation). */
class HeldPetalRotation {
    private var currentHand: Quat? = null
    private var anchorHand: Quat? = null
    private var anchorPetal: Quat? = null
    var rotation: Quat? = null; private set

    fun observe(hand: Quat?) {
        currentHand = hand?.takeIf { it.isFinite() }?.normalize()
        val current = currentHand ?: return
        val petal = anchorPetal ?: return
        val anchor = anchorHand
        if (anchor == null) {
            // No pose at detach: the first available pose anchors without snapping the petal.
            anchorHand = current
            return
        }
        rotation = (current * anchor.conjugate() * petal).normalize()
    }

    fun detach(petal: Quat) {
        anchorPetal = petal.normalize()
        anchorHand = currentHand
        rotation = anchorPetal
    }

    fun clear() {
        currentHand = null
        anchorHand = null
        anchorPetal = null
        rotation = null
    }
}
