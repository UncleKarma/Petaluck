package com.example.flowertears.p18ab52cf.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.example.flowertears.p18ab52cf.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
