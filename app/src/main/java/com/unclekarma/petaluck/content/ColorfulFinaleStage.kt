package com.unclekarma.petaluck.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pico.spatial.ui.foundation.content.SpatialView
import com.unclekarma.petaluck.domain.RoseLayout
import com.unclekarma.petaluck.platform.SpatialApplication
import com.unclekarma.petaluck.scene.EndingPetalRain
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The Stage plays the surrounding rain and its soundtrack. The volume owns the wish and exit. */
@Composable
fun ColorfulFinaleStage() {
    val app = LocalContext.current.applicationContext as SpatialApplication
    val rain = remember { EndingPetalRain(app) }
    val music = remember { app.finaleMusic }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var rainStarted by remember { mutableStateOf(false) }
    DisposableEffect(rain) { onDispose { rain.close() } }
    if (rainStarted && music != null) {
        DisposableEffect(music, lifecycle) {
            val observer = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = music.play()
                override fun onStop(owner: LifecycleOwner) = music.pause()
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                music.releasePlayer()
            }
        }
    }
    SpatialView(
        modifier=Modifier,
        initial={content,_->
            scope.launch {
                val config=RoseLayout.assemble(0x46494e414cL,24,RoseLayout.MULTICOLOR)
                rain.prepare(1,config,surrounding=true)
                if (!rain.start(1,content,endless=true)) return@launch
                rainStarted = true
                // One shared random total each second; disposing the Stage cancels this loop.
                while (isActive) {
                    delay(1_000)
                    if (!rain.randomizeFinaleEmissionRate()) break
                }
            }
        },
    )
}
