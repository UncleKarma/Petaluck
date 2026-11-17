package com.unclekarma.petaluck

import androidx.compose.ui.Modifier
import com.unclekarma.petaluck.content.FlowerVolume
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.Stage
import com.pico.spatial.ui.foundation.dsl.Immersion
import com.unclekarma.petaluck.content.ColorfulFinaleStage
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.platform.meters

fun mainApp(scope: SpatialAppScope) = with(scope) {
    Stage(id="petaluck_finale", immersion=Immersion(0,0,0)) {
        PicoTheme { ColorfulFinaleStage() }
    }
    DefaultWindowContainer {
        PicoTheme {
            FlowerVolume(Modifier.windowConstraints(width=.60f.meters,height=.70f.meters,depth=.40f.meters))
        }
    }
}
