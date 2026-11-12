package com.example.flowertears.p18ab52cf.domain

/** Gesture displacement and detached pose share RoseAnchor's meter axes at full entrance scale. */
class PetalDragMotion {
    var delta = Vec(); private set
    private var detachedOrigin: Vec? = null
    val detachedPosition: Vec? get() = detachedOrigin?.let { it + delta }

    fun move(displacement: Vec) { delta = displacement }

    /** Preserve the visible root-relative pose when the petal stops inheriting crown motion. */
    fun detach(position: Vec) { detachedOrigin = position - delta }

    fun clear() { delta = Vec(); detachedOrigin = null }
}
