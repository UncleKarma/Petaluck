package com.unclekarma.petaluck

import androidx.compose.ui.Modifier
import com.unclekarma.petaluck.content.FlowerVolume
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.platform.meters

fun mainApp(scope: SpatialAppScope) = with(scope) {
    DefaultWindowContainer {
        PicoTheme {
            FlowerVolume(Modifier.windowConstraints(width=.60f.meters,height=.70f.meters,depth=.40f.meters))
        }
    }
}
