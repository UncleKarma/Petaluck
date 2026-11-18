package com.unclekarma.petaluck.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.pico.spatial.ui.foundation.content.SpatialView
import com.unclekarma.petaluck.domain.RoseLayout
import com.unclekarma.petaluck.scene.EndingPetalRain
import kotlinx.coroutines.launch

/** Stage owns only the surrounding rain. The original volume owns the sole wish and exit button. */
@Composable
fun ColorfulFinaleStage() {
    val context=LocalContext.current
    val rain=remember { EndingPetalRain(context.applicationContext) }
    val scope=rememberCoroutineScope()
    DisposableEffect(rain) { onDispose { rain.close() } }
    SpatialView(
        modifier=Modifier,
        initial={content,_->
            scope.launch {
                val config=RoseLayout.assemble(0x46494e414cL,24,RoseLayout.MULTICOLOR)
                rain.prepare(1,config,surrounding=true)
                rain.start(1,content,endless=true)
            }
        },
    )
}
