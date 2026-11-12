package com.example.flowertears.p18ab52cf.scene

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/** Readiness follows real SpatialView scene frames, including frames before a waiter starts. */
class SceneFrameGate {
    private val frames = MutableStateFlow(0L)

    fun onFrame() { frames.value += 1 }

    suspend fun awaitFirstFrame() { frames.first { it > 0L } }

    suspend fun awaitNextFrame() {
        val current = frames.value
        frames.first { it > current }
    }

    suspend fun <T : Any> awaitReady(maxFrames: Int = 600, read: () -> T?): T {
        require(maxFrames > 0)
        awaitFirstFrame()
        val start = frames.value
        var observed = start
        while (true) {
            read()?.let { return it }
            check(observed - start < maxFrames) { "Scene content not ready after $maxFrames active frames" }
            observed = frames.first { it > observed }
        }
    }
}
