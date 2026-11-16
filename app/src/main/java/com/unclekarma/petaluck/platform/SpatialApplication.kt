package com.unclekarma.petaluck.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.unclekarma.petaluck.mainApp

class SpatialApplication : Application() {
    val debugRoseColor = com.unclekarma.petaluck.domain.DebugRoseColor()
    override fun onCreate() {
        super.onCreate()
        RoseColorDebugReceiver.register(this, debugRoseColor)
        launch(::mainApp)
    }
}
