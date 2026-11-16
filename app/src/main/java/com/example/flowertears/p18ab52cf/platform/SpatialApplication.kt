package com.example.flowertears.p18ab52cf.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.example.flowertears.p18ab52cf.mainApp

class SpatialApplication : Application() {
    val debugRoseColor = com.example.flowertears.p18ab52cf.domain.DebugRoseColor()
    override fun onCreate() {
        super.onCreate()
        RoseColorDebugReceiver.register(this, debugRoseColor)
        launch(::mainApp)
    }
}
