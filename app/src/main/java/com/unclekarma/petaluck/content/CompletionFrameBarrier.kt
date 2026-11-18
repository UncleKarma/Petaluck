package com.unclekarma.petaluck.content

import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.unclekarma.petaluck.domain.CompletionBufferGate

/**
 * Register only from this panel's requested draw, not the ViewTree's earlier OnDraw notification.
 * Blank draws exclude retained child layers. Blessing draws submit this round’s text under the native mask.
 * Android submission is not a spatial compositor presentation fence; erase on exit as well.
 */
@Composable
internal fun completionFrameBarrier(
    request: CompletionBufferGate.Request?,
    onCommitted: (CompletionBufferGate.Request) -> Unit,
): Modifier {
    val view = LocalView.current
    val submission = remember(view, request) {
        request?.let { FrameSubmission(view) { onCommitted(it) } }
    }
    DisposableEffect(submission) {
        view.invalidate()
        onDispose { submission?.dispose() }
    }
    return Modifier.drawWithContent {
        // Transparent SrcOver does not erase retained pixels. Clear the target explicitly.
        drawRect(Color.Transparent, blendMode = BlendMode.Clear)
        if (request?.draw != CompletionBufferGate.Draw.Blank) drawContent()
        submission?.drawn()
    }
}

private class FrameSubmission(private val view: View, private val committed: () -> Unit) {
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
