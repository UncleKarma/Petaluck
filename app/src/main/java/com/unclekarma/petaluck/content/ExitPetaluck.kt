package com.unclekarma.petaluck.content

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.pico.spatial.ui.platform.containers.SpatialNavigator
import com.unclekarma.petaluck.platform.SpatialApplication
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Spatial content may have no Activity. Close its native containers before finishing app tasks. */
internal suspend fun exitPetaluck(context: Context, navigator: SpatialNavigator) = withContext(NonCancellable) {
    (context.applicationContext as SpatialApplication).endFinale()
    // Closing a container disposes its composition and cancels its scope. Finish all cleanup anyway.
    try {
        navigator.closeStage()
    } catch (e: Exception) {
        Log.e("Petaluck", "Could not close finale Stage", e)
    }
    try {
        navigator.closeWindowContainer()
    } catch (e: Exception) {
        Log.e("Petaluck", "Could not close flower window", e)
    }
    try {
        // Android returns only tasks belonging to this application.
        context.getSystemService(ActivityManager::class.java)?.appTasks?.forEach { task ->
            try { task.finishAndRemoveTask() }
            catch (e: Exception) { Log.e("Petaluck", "Could not finish app task", e) }
        }
        context.findActivity()?.finishAndRemoveTask()
        Log.i("Petaluck", "PetaluckExitRequested stageAndWindow=true")
    } catch (e: Exception) {
        Log.e("Petaluck", "Could not finish app activities", e)
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext !== this) baseContext.findActivity() else null
    else -> null
}
