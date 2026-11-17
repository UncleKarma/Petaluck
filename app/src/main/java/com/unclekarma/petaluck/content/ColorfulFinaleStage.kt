package com.unclekarma.petaluck.content

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.AccessibilityComponent
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.content.setAccessibilityActions
import com.unclekarma.petaluck.domain.RoseBlessings
import com.unclekarma.petaluck.domain.RoseLayout
import com.unclekarma.petaluck.scene.EndingPetalRain
import kotlinx.coroutines.launch

/** Final scene is an unbounded Mixed Stage, separate from the 60 cm flower window. */
@Composable
fun ColorfulFinaleStage() {
    val context=LocalContext.current
    val rain=remember { EndingPetalRain(context.applicationContext) }
    val scope=rememberCoroutineScope()
    var panelReady by remember { mutableStateOf(false) }
    var panelBound by remember { mutableStateOf(false) }
    DisposableEffect(rain) { onDispose { rain.close() } }
    SpatialView(
        modifier=Modifier,
        attachments={
            if(panelReady) AttachmentPanel(id="petaluck_finale_message") {
                Box(Modifier.width(900.dp).height(500.dp), contentAlignment=Alignment.Center) {
                    androidx.compose.foundation.layout.Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(RoseBlessings.COLORFUL_FINALE,
                            style=PicoTheme.typography.headlineMedium,
                            fontSize=PicoTheme.typography.headlineMedium.fontSize*2,
                            lineHeight=PicoTheme.typography.headlineMedium.lineHeight*2,
                            color=roseTextWhite,textAlign=TextAlign.Center,maxLines=4)
                        Button(onClick={context.findActivity()?.finishAffinity()},
                            colors=ButtonDefaults.buttonColors(contentColor=roseTextWhite)) {
                            Text("就此别过")
                        }
                    }
                }
            }
        },
        update={content,attachments->
            attachments.entity("petaluck_finale_message")?.takeIf { !panelBound }?.let { panel->
                panel.components.set(AccessibilityComponent("最后的祝福与退出按钮",RoseBlessings.COLORFUL_FINALE))
                panel.components[TransformComponent::class.java]?.setPosition(Vector3(0f,1.55f,-1.2f))
                content.addEntity(panel)
                content.setAccessibilityActions(panel,"rose.finale") {}
                panelBound=true
            }
        },
        initial={content,_->
            content.subscribe(com.pico.spatial.core.ecs.event.SceneEvents.Update::class.java) {
                if(!panelReady) panelReady=true
            }
            scope.launch {
                val config=RoseLayout.assemble(0x46494e414cL,24,RoseLayout.MULTICOLOR)
                rain.prepare(1,config,surrounding=true)
                rain.start(1,content,endless=true)
            }
        },
    )
}

internal fun Context.findActivity(): Activity? = when(this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
