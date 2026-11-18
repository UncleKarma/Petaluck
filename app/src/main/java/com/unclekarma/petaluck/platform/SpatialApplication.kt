package com.unclekarma.petaluck.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.unclekarma.petaluck.mainApp
import com.unclekarma.petaluck.scene.FinaleMusic

class SpatialApplication : Application() {
    val debugRoseColor = com.unclekarma.petaluck.domain.DebugRoseColor()
    // The volume owns the exit button, while the Stage knows when rain actually starts.
    internal var finaleMusic: FinaleMusic? = null
        private set

    internal fun beginFinale() {
        endFinale()
        finaleMusic = FinaleMusic(this)
    }

    internal fun endFinale() {
        finaleMusic?.close()
        finaleMusic = null
    }

    override fun onCreate() {
        super.onCreate()
        RoseColorDebugReceiver.register(this, debugRoseColor)
        launch(::mainApp)
    }
}
