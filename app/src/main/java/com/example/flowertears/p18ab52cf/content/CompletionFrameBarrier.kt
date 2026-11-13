package com.example.flowertears.p18ab52cf.content

import android.util.Log
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView

/**
 * Wait for this attachment's transparent frame to reach its swap chain.
 * Composition, layout and a fixed number of scene frames do not confirm buffer submission.
 */
@Composable
internal fun CompletionFrameBarrier(pending: Boolean, onCommitted: () -> Unit) {
    val view = LocalView.current
    val committed = rememberUpdatedState(onCommitted)
    DisposableEffect(view, pending) {
        if (!pending) return@DisposableEffect onDispose {}
        val observer = view.viewTreeObserver
        var active = true
        var registered = false
        val callback = Runnable {
            // Frame-commit callbacks may run on the render thread.
            view.post { if (active) committed.value() }
        }
        val draw = ViewTreeObserver.OnDrawListener {
            if (!registered) {
                registered = true
                if (view.isHardwareAccelerated) {
                    observer.registerFrameCommitCallback(callback)
                } else {
                    // Spatial attachments use hardware rendering; never reveal a stale buffer.
                    Log.e("FlowerTears", "Completion frame barrier requires hardware rendering")
                }
            }
        }
        observer.addOnDrawListener(draw)
        view.invalidate()
        onDispose {
            active = false
            if (observer.isAlive) {
                observer.removeOnDrawListener(draw)
                observer.unregisterFrameCommitCallback(callback)
            }
        }
    }
}
