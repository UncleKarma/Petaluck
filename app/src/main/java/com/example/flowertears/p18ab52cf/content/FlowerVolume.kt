package com.example.flowertears.p18ab52cf.content

import android.view.ViewTreeObserver
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.flowertears.p18ab52cf.domain.Phase
import com.example.flowertears.p18ab52cf.scene.RoseScene
import com.example.flowertears.p18ab52cf.scene.RoseUi
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialPointerEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun FlowerVolume(modifier:Modifier) {
    val context=LocalContext.current
    val scene=remember {RoseScene(context.applicationContext)}
    val panelsReady=scene.attachmentsReady
    val scope=rememberCoroutineScope()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val view=LocalView.current
    // Host lifecycle/view bindings may change while this remembered scene remains in composition.
    DisposableEffect(scene) {onDispose {scene.close()}}
    DisposableEffect(scene,lifecycle,view) {
        val observer=LifecycleEventObserver {_,event->
            when(event) {
                Lifecycle.Event.ON_PAUSE,Lifecycle.Event.ON_STOP->scene.pause()
                Lifecycle.Event.ON_RESUME->scene.resume()
                else->Unit
            }
        }
        // Spatial attachments can take focus without pausing the host activity.
        val focus=ViewTreeObserver.OnWindowFocusChangeListener {focused->if(!focused)scene.cancelInput()}
        lifecycle.addObserver(observer);view.viewTreeObserver.addOnWindowFocusChangeListener(focus)
        onDispose {lifecycle.removeObserver(observer);if(view.viewTreeObserver.isAlive)view.viewTreeObserver.removeOnWindowFocusChangeListener(focus)}
    }
    SpatialView(
        modifier=modifier.pointerInput(scene) {
            try {
                detectSpatialPointerEvent(context,targetedToEntity=TargetEntity.any {scene.isPetalTarget(it)}) {scene.pointer(it)}
            } finally {scene.streamLost()}
        },
        attachments={
            // Creating a native attachment before its first surface frame can cache an empty owner.
            if(panelsReady) {
                AttachmentPanel(id="rose_result") {
                    val ui=scene.ui
                    Box(Modifier.width(560.dp).height(104.dp).semantics {liveRegion=LiveRegionMode.Polite},contentAlignment=Alignment.Center) {
                        var displayed by remember(ui.roundId) {mutableStateOf("")}
                        var targetAlpha by remember(ui.roundId) {mutableStateOf(0f)}
                        LaunchedEffect(ui.roundId,ui.result) {
                            targetAlpha=0f
                            if(ui.result.isEmpty()){displayed="";return@LaunchedEffect}
                            delay(90);displayed=ui.result;targetAlpha=1f
                        }
                        val alpha by animateFloatAsState(targetAlpha,tween(90),label="resultFade")
                        // Empty/new-round content must not wait for an effect or a fade-out.
                        RoseLargeText(text=if(ui.result.isEmpty()) "" else displayed,modifier=Modifier.alpha(alpha))
                    }
                }
                AttachmentPanel(id="rose_message") {
                    val ui=scene.ui
                    val completing=ui.phase==Phase.Completing || ui.phase==Phase.Completed
                    val replay = { scope.launch(start=CoroutineStart.UNDISPATCHED) {scene.replay(ui.roundId)}; Unit }
                    // One native layout keeps the message's transparent hit area off the button.
                    Column(Modifier.width(560.dp).height(128.dp),horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.spacedBy(16.dp,Alignment.CenterVertically)) {
                        when {
                            ui.phase==Phase.Loading->Text("花正在盛开…",style=PicoTheme.typography.bodyLarge,color=roseTextWhite)
                            ui.phase==Phase.Error->{
                                Text("这朵花暂时没准备好，请再试一次。",style=PicoTheme.typography.bodyMedium,color=roseTextWhite)
                                ReplayButton("重新开始", enabled=true, onClick=replay)
                            }
                            completing->Unit
                            ui.trackingLost->Text("暂时没看清你的手，放松一下，再试试。",style=PicoTheme.typography.bodyMedium,color=roseTextWhite)
                            ui.teaching->Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Text("心里想着一个人，也可以只为自己。",style=PicoTheme.typography.bodyMedium,color=roseTextWhite)
                                Text("看向或靠近一片花瓣，捏住，轻轻拉开。",style=PicoTheme.typography.bodyMedium,color=roseTextWhite)
                            }
                        }
                    }
                }
                // Keep each native surface at one position. Moving a surface while Compose
                // replaces its buffer can display the previous text at the next phase's position.
                AttachmentPanel(id="rose_completion") {
                    val ui=scene.ui
                    val completing=ui.phase==Phase.Completing || ui.phase==Phase.Completed
                    val presented=scene.completionPresentedRound==ui.roundId
                    val replay = { scope.launch(start=CoroutineStart.UNDISPATCHED) {scene.replay(ui.roundId)}; Unit }
                    // Stable surface size also avoids a resize racing the outgoing text buffer.
                    // Center the original wish/button group inside it without changing its spacing.
                    Box(Modifier.width(560.dp).height(384.dp),contentAlignment=Alignment.Center) {
                        if(completing) key(ui.roundId) {
                            CompletionFrameBarrier(
                                pending=!presented,
                                onCommitted={scene.completionFrameCommitted(ui.roundId)},
                            )
                            CompletionFooter(ui,presented,replay)
                        }
                    }
                }
            }
        },
        update={content,attachments->
            // Idempotent binding also handles panels that were absent during initial composition.
            scene.bindPanels(content,attachments)
        },
        initial={content,attachments->
            scene.initialize(content,scope,attachments)
        }
    )
}

@Composable
private fun CompletionFooter(ui: RoseUi, presented: Boolean, onReplay: () -> Unit) {
    val completed = ui.phase == Phase.Completed
    val blessingFade = remember { Animatable(0f) }
    val replayFade = remember { Animatable(0f) }
    // The first committed buffer must be transparent, even on round two and later.
    LaunchedEffect(presented) {
        if(presented) blessingFade.animateTo(1f,tween(600)) else blessingFade.snapTo(0f)
    }
    LaunchedEffect(completed,presented) {
        if(completed && presented) replayFade.animateTo(1f,tween(450)) else replayFade.snapTo(0f)
    }
    val canReplay = presented && completed && replayFade.value >= 1f
    val buttonHeight = 48.dp // SpatialUI Regular button height.

    Box(Modifier.fillMaxWidth()) {
        // Measure the wish with room already deducted for the button and the gap.
        RoseLargeText(ui.blessing, modifier = Modifier.fillMaxWidth()
            .padding(bottom = buttonHeight + 24.dp).alpha(if(presented)blessingFade.value else 0f), maxLines = 3)
        Box(Modifier.align(Alignment.BottomCenter).height(buttonHeight).alpha(if(presented)replayFade.value else 0f)
            .then(if (canReplay) Modifier else Modifier.clearAndSetSemantics {}),
            contentAlignment = Alignment.Center) {
            ReplayButton("再试一次", enabled = completed, onClick = { if (canReplay) onReplay() })
        }
    }
}

@Composable
private fun ReplayButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, colors = ButtonDefaults.buttonColors(contentColor = roseTextWhite)) {
        Text(label)
    }
}

@Composable
private fun RoseLargeText(text:String,modifier:Modifier=Modifier,maxLines:Int=1) {
    // User: completion blessing must match the result text exactly (80/104sp in SDK 6.2.8).
    Text(text=text,modifier=modifier,style=PicoTheme.typography.headlineMedium,
        fontSize=PicoTheme.typography.headlineMedium.fontSize*4,
        lineHeight=PicoTheme.typography.headlineMedium.lineHeight*4,
        color=roseTextWhite,textAlign=TextAlign.Center,maxLines=maxLines,softWrap=maxLines>1)
}

private val roseTextWhite = Color(0xFFFFFFFF) // design-style: fixed-figma-color User request 2026-11-10: result and all footer text are white.
