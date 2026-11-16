package com.unclekarma.petaluck.domain

import kotlin.math.PI
import kotlin.math.tan

/** Authored GLB bind-chain coordinates, verified against the exported asset bytes. */
object RoseStem {
    const val LENGTH = .24f
    const val LEAN = .008f
    private const val NECK_START = .64f
    private const val BODY_BOW = .0035f
    private val neckBow = ((LENGTH * tan(RoseLayout.CORE_PITCH * PI / 180) - 2 * BODY_BOW) *
        (1f - NECK_START) / 3).toFloat()
    val root = Vec(LEAN, -LENGTH, -BODY_BOW - neckBow)
    val mid = Vec(LEAN * .25f, -LENGTH * .5f, -.75f * BODY_BOW - neckBow)
    val tip = Vec()
    val midLink = mid - root
    val tipLink = tip - mid
}
