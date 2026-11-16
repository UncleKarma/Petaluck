package com.unclekarma.petaluck.content

import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalView
import com.unclekarma.petaluck.domain.CompletionBufferGate

/**
 * Register only from this panel's blank draw, not the ViewTree's earlier OnDraw notification.
 * Skipping drawContent also excludes retained child graphics layers (including the native button).
 * Android submission is not a spatial compositor presentation fence; erase on exit as well.
 */
@Composable
internal fun completionFrameBarrier(
    request: CompletionBufferGate.Request?,
    onCommitted: (CompletionBufferGate.Request) -> Unit,
): Modifier {
    val view = LocalView.current
    val submission = remember(view, request) {
        request?.let { BlankSubmission(view) { onCommitted(it) } }
    }
    DisposableEffect(submission) {
        view.invalidate()
        onDispose { submission?.dispose() }
    }
    return Modifier.drawWithContent {
        if (submission == null) drawContent() else submission.drawn()
    }
}

private class BlankSubmission(private val view: View, private val committed: () -> Unit) {
    private var active = true
    private var registered = false
    private var observer = view.viewTreeObserver
    private val callback = Runnable {
        // The render-thread callback only posts work; all state/Entity access stays on main.
        view.post { if (active) committed() }
    }

    fun drawn() {
        if (!active || registered) return
        registered = true
        observer = view.viewTreeObserver
        if (view.isHardwareAccelerated && observer.isAlive) {
            observer.registerFrameCommitCallback(callback)
        } else {
            Log.e("Petaluck", "Completion frame barrier requires hardware rendering")
        }
    }

    fun dispose() {
        active = false
        if (observer.isAlive) observer.unregisterFrameCommitCallback(callback)
    }
}
