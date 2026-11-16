package com.unclekarma.petaluck.platform

import android.os.Bundle
import android.util.Log
import com.pico.spatial.ui.platform.stub.SpatialLaunchActivity

class LaunchActivity : SpatialLaunchActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            (application as SpatialApplication).debugRoseColor.reset()
            Log.i("RoseDebug", "New launch: selectedColor=-1 (random)")
        }
        super.onCreate(savedInstanceState)
    }
}
