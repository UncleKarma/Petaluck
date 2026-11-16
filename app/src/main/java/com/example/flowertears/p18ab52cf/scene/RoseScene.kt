package com.example.flowertears.p18ab52cf.scene

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.flowertears.p18ab52cf.domain.*
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.event.SceneEvents
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.lifecycle.Cancellable
import com.pico.spatial.core.math.*
import com.pico.spatial.ui.foundation.content.clearAccessibilityActions
import com.pico.spatial.ui.foundation.content.setAccessibilityActions
import com.pico.spatial.ui.foundation.content.SpatialViewAttachments
import com.pico.spatial.ui.foundation.gesture.SpatialPointerInfo
import com.pico.spatial.ui.foundation.gesture.data.InteractionKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*
import kotlin.random.Random

data class RoseUi(val phase: Phase = Phase.Loading, val result: String = "", val teaching: Boolean = true, val trackingLost: Boolean = false, val blessing: String = "", val roundId: Long = 0)

/** All 3D writes happen on the main thread, in input callbacks or scene update events. */
class RoseScene(context: Context) {
    private val debugRoseColor = (context.applicationContext as com.example.flowertears.p18ab52cf.platform.SpatialApplication).debugRoseColor
    var ui by mutableStateOf(RoseUi()); private set
    var attachmentsReady by mutableStateOf(false); private set
    var completionPresentedRound by mutableStateOf<Long?>(null); private set
    var completionBlankRequest by mutableStateOf<CompletionBufferGate.Request?>(null); private set
    private val completionBuffer = CompletionBufferGate()
    private lateinit var content: SpatialViewContent
    private lateinit var sceneScope: CoroutineScope
    private var subscription: Cancellable? = null
    private val frameGate = SceneFrameGate()
    private var initializationJob: Job? = null
    private var assemblyJob: Job? = null
    private var audioPreparationJob: Job? = null
    private val prototypes = mutableMapOf<String,Entity>()
    // A Kotlin reference does not retain native resources after the last petal is destroyed.
    private val textures = RoundAssetSet<TextureResource>({it.valid},{it.toGlobal()},{it.close()})
    private val petalAudio = PetalSpatialAudio(context.applicationContext)
    private val endingPetalRain = EndingPetalRain(context.applicationContext)
    private val endingMusic = EndingMusic(context.applicationContext,
        onStarted = { roundId ->
            if(!disposed && !appPaused && roundId==generation)endingPetalRain.start(roundId,content)
        },
        onStopped = { endingPetalRain.stop() },
        onFinished = { /* Rain owns its 15 s deadline, beyond the 14.5 s audio clip. */ },
    )
    private val petalSampler = MaterialTextureSampler().apply {sWrapMode=SamplerWrapMode.CLAMP_TO_EDGE;tWrapMode=SamplerWrapMode.CLAMP_TO_EDGE}
    private var root: Entity? = null
    private var stem: Entity? = null
    private var crown: Entity? = null
    private var skeleton: SkeletonPoseComponent? = null
    private var restPose = emptyList<Transform>()
    private var midIndex = -1
    private var tipIndex = -1
    private var session: RoseSession? = null
    private val recentColors = RecentRoseColors()
    private var generation = 0L
    private var disposed = false
    private var busy = false
    private val petals = mutableMapOf<Int,PetalVisual>()
    private val pinchDrag = PinchDragTracker()
    private val petalDrag = PetalDragMotion()
    private val remotePetalDrag = RemotePetalDrag()
    private val heldPetalRotation = HeldPetalRotation()
    private var lostTime = -1f
    private val stemMotion = StemMotion()
    private val entrance = RoseEntrance()
    private var entranceAwaitingFrame = false
    private var appPaused = false
    private var trackingUnavailable = false
    private val coreMaterials = mutableListOf<Pair<UnlitMaterial,Color4>>()
    private var glowTime = 0f
    private var resultPanel:Entity? = null
    private var messagePanel:Entity? = null
    private var completionPanel:Entity? = null
    private val resultBinding = AttachmentBinding<Entity> {it.valid}
    private val messageBinding = AttachmentBinding<Entity> {it.valid}
    private val completionBinding = AttachmentBinding<Entity> {it.valid}
    private data class PetalVisual(val spec: PetalSpec, val owner: Entity, val meshOwner: Entity, val material: UnlitMaterial,
        val shape: ShapeResource, val rest: Transform, var falling: Float = -1f, var fallStart: Transform? = null,
        var returning: Float = -1f, var returnStart: Transform? = null)

    fun bindPanels(content:SpatialViewContent,attachments:SpatialViewAttachments):Boolean {
        if(disposed)return false
        val resultPanel=attachments.entity("rose_result")
        val messagePanel=attachments.entity("rose_message")
        val completionPanel=attachments.entity("rose_completion")
        resultBinding.bind(resultPanel) {
            if(resultPanel!=null) {
                resultPanel.components.set(AccessibilityComponent("占卜回应","尚未摘取"))
                resultPanel.enabled=false
                resultPanel.components[TransformComponent::class.java]!!.setPosition(Vector3(0f,.175f,.075f))
                content.addEntity(resultPanel)
                content.setAccessibilityActions(resultPanel,"rose.result") {}
            }
        }
        messageBinding.bind(messagePanel) {
            if(messagePanel!=null) {
                messagePanel.components.set(AccessibilityComponent("提示与重玩操作区","花正在盛开"))
                messagePanel.components[TransformComponent::class.java]!!.setPosition(Vector3(0f,-.245f,.075f))
                content.addEntity(messagePanel)
                content.setAccessibilityActions(messagePanel,"rose.message") {}
            }
        }
        completionBinding.bind(completionPanel) {
            if(completionPanel!=null) {
                completionPanel.enabled=false
                completionBuffer.reset()
                completionPresentedRound=null
                completionBlankRequest=null
                completionPanel.components.set(OpacityControllerComponent(0f))
                completionPanel.components.set(AccessibilityComponent("祝福与重玩操作区",""))
                completionPanel.components[TransformComponent::class.java]!!.setPosition(Vector3(0f,-.05f,.12f))
                content.addEntity(completionPanel)
                content.setAccessibilityActions(completionPanel,"rose.completion") {}
            }
        }
        this.resultPanel=resultPanel?.takeIf {it.valid}
        this.messagePanel=messagePanel?.takeIf {it.valid}
        this.completionPanel=completionPanel?.takeIf {it.valid}
        syncPanels(ui)
        return this.resultPanel!=null && this.messagePanel!=null && this.completionPanel!=null
    }
    private suspend fun ensureAssets(config:RoundConfig) {
        textures.retain(RoseLayout.texturePaths(config)) {path->withContext(Dispatchers.IO){TextureResource.load(path,option=TextureCreateOption())}}
        if(disposed){textures.close();return}
        for(variant in RoseLayout.variantIds)if(prototypes[variant]?.valid!=true) {
            val entity=Entity.loadSuspend("asset://rose/$variant.glb")
            if(disposed){entity.destroy();return}
            prototypes[variant]=entity
        }
    }
    fun initialize(c: SpatialViewContent,scope:CoroutineScope,attachments:SpatialViewAttachments) {
        if(disposed || initializationJob!=null)return
        content = c
        sceneScope = scope
        subscription = c.subscribe(SceneEvents.Update::class.java) {
            // Readiness must advance even while gameplay is paused or a round is loading.
            frameGate.onFrame()
            if(!attachmentsReady)attachmentsReady=true
            try {tick(it.deltaTime.coerceIn(0f,.05f))}
            catch(e:Exception) {Log.e(TAG,"Scene update failed",e);session?.fail();publish()}
        }
        initializationJob=scope.launch {
            try {
                // Cold startup can compose SpatialView before its native surface starts producing frames.
                frameGate.awaitFirstFrame()
                // Attachment display owners must be created after the native surface is ready.
                // Resolve again on real frames if Compose publishes the panels later than initial.
                frameGate.awaitReady {
                    if(bindPanels(c,attachments))Unit else null
                }
                Log.i(TAG,"SpatialView first frame received; starting initial round")
                newRound()
            } catch(e:CancellationException) {
                throw e
            } catch(e:Exception) {
                if(!disposed) {
                    Log.e(TAG,"Asset initialization failed",e);session?.fail();updateUi(RoseUi(Phase.Error))
                }
            }
        }
    }
    suspend fun replay(roundId: Long) {
        // Reject stale/duplicate clicks after loading or the next round has begun.
        if(ui.roundId!=roundId || (ui.phase!=Phase.Completed && ui.phase!=Phase.Error))return
        if(disposed || busy || appPaused)return
        endingPetalRain.stop()
        endingMusic.replay()
        newRound()
    }
    private suspend fun newRound() {
        if (disposed || busy) return
        busy = true
        val job=currentCoroutineContext()[Job]
        assemblyJob=job
        updateUi(RoseUi(Phase.Loading))
        // Error retry preserves legal sampled parameters; completed replay draws anew.
        val config = debugRoseColor.sample(Random.nextLong(), recentColors, session?.takeIf { it.phase == Phase.Error }?.config)
        val token = ++generation
        var stage = "clear previous round"
        try {
            clearRound()
            stage = "retain shared assets"
            ensureAssets(config)
            if(disposed)return
            val s = RoseSession(token, config); session=s
            // Hide the complete hierarchy during assembly without disabling native rig updates.
            val anchor=Entity().apply {
                setName("RoseAnchor")
                components.set(TransformComponent(Vector3(0f,RoseEntrance.ROOT_OFFSET_Y,0f),Quat.identity(),Vector3.ONE))
                components.set(OpacityControllerComponent(0f))
            }
            root=anchor;content.addEntity(anchor)
            stage = "load stem"
            val base=Entity.loadSuspend("asset://rose/stem.glb")
            if(disposed || token!=generation){base.destroy();return}
            stem=base;anchor.addChild(base);base.components[TransformComponent::class.java]!!.setPosition(Vector3(0f,.04f,0f))
            val coreNodes=mutableListOf<Entity>()
            for(node in walk(base)) {
                val model=node.components[ModelComponent::class.java] ?: continue
                if(node.getName() in setOf("Hypanthium_and_five_sepals","Filaments","Anthers_and_pistils")) {
                    coreNodes+=node
                }
                if(node.getName().contains("Anthers"))for(i in 0 until model.materials.size) {
                    val mat=model.materials[i] as? UnlitMaterial ?: continue
                    coreMaterials+=mat to mat.getBaseColor()
                }
            }
            val skinned=base.findSkinnedMeshEntity().singleOrNull() ?: error("Stem must have exactly one skinned mesh")
            val pose=SkeletonPoseComponent();skinned.components.set(pose)
            stage = "wait for stem rig"
            // Imported skeletons may become available after the component's attachment frame.
            val (names,transforms)=awaitStemPose(pose,token)
            midIndex=names.indexOfFirst { it.substringAfterLast('/')=="stem_mid" }
            tipIndex=names.indexOfFirst { it.substringAfterLast('/')=="stem_tip" }
            val rootIndex=names.indexOfFirst { it.substringAfterLast('/')=="stem_root" }
            require(pose.getJointCount()==3 && rootIndex>=0 && midIndex>=0 && tipIndex>=0) {"Missing three-bone stem rig"}
            restPose=transforms;require(restPose.size==3);skeleton=pose
            val top=Entity().apply {setName("CrownMotion");components.set(TransformComponent(Vector3(0f,.04f,0f),EulerAngles(RoseLayout.HEAD_PITCH,0f,0f),Vector3.ONE))}
            crown=top;anchor.addChild(top)
            require(coreNodes.size==3) {"Missing flower core parts"}
            for(node in coreNodes) {
                node.removeFromParent();top.addChild(node)
                // Share the same crown transform during rest, bending, detach and reset.
                node.components.set(TransformComponent(Vector3.ZERO,EulerAngles(RoseLayout.CORE_LOCAL_PITCH,0f,0f),Vector3.ONE))
            }
            for (spec in config.petals) {
                stage = "assemble petal ${spec.id} (${spec.variant})"
                val owner=prototypes.getValue(spec.variant).clone(Entity.CloneOptions(recursive=true)) ?: error("Petal clone failed")
                top.addChild(owner);owner.setName("Petal_${spec.id}")
                val rest=Transform(spec.position.vector(),petalRotation(spec),Vector3.ONE*spec.scale)
                owner.components.set(TransformComponent(rest))
                val meshOwner=walk(owner).firstOrNull {it.components.has(ModelComponent::class.java)} ?: error("Missing petal mesh")
                val model=meshOwner.components[ModelComponent::class.java]!!
                // Full-color albedo preserves pale roots and richer rims without tinting them twice.
                val material=UnlitMaterial.create().apply {
                    val shade=spec.albedoScale
                    setBaseColor(Color4(shade,shade,shade,1f))
                    setBaseColorTextureParameter(MaterialTextureParameter(
                        checkNotNull(textures[RoseLayout.texturePath(spec)]){"Layered rose texture unavailable"},
                        petalSampler, spec.uvIndex))
                    setCullingMode(MaterialCullingMode.NONE)
                }
                for(i in 0 until model.materials.size) model.materials[i]=material
                // Exact mesh shape preserves frontmost occlusion, rather than broad overlapping boxes.
                // Retain the shape while the entrance deliberately has no CollisionComponent.
                val shape=ShapeResource.createStaticMesh(model.mesh).apply {toGlobal()}
                // Keep colliders, hover and all click/gesture actions absent until full scale.
                val visual=PetalVisual(spec,owner,meshOwner,material,shape,rest);petals[spec.id]=visual
            }
            config.validate();require(petals.size==config.count)
            if(appPaused)s.pause()
            applyEntrance(entrance.start())
            publish() // Still Loading: every input path remains locked for the entrance.
            Log.i(TAG,"Round assembled id=$token seed=${config.seed} N=${config.count} color=${config.colorId} layout=${RoseLayout.VERSION}; entering")
        } catch(e:CancellationException) {
            throw e
        } catch(e:Exception) {
            if(disposed)return
            Log.e(TAG,"Round assembly failed session=$token seed=${config.seed} stage=$stage",e);clearRound();session=RoseSession(token,config).apply{fail()};publish()
        } finally {busy=false;if(assemblyJob===job)assemblyJob=null}
    }
    private suspend fun awaitStemPose(pose:SkeletonPoseComponent,token:Long):Pair<List<String>,List<Transform>> {
        return frameGate.awaitReady {
            if(disposed || token!=generation)throw CancellationException("Round no longer active")
            val names=pose.getJointNames()
            val transforms=pose.getJointTransforms()
            if(pose.getJointCount()==3 && names.size==3 && transforms.size==3)names to transforms else null
        }
    }
    private fun petalRotation(spec:PetalSpec,extraTilt:Float=0f):Quat {
        // Side-tilt first, lift in the radial plane, then distribute around the growth axis.
        val radians=PI.toFloat()/180f
        return Quat(Vector3(0f,0f,1f),spec.angle*radians) *
            Quat(Vector3(1f,0f,0f),(spec.tilt+extraTilt)*radians) *
            Quat(Vector3(0f,1f,0f),spec.roll*radians)
    }
    private fun applyEntrance(frame:RoseEntrance.Frame) {
        root?.components?.get(TransformComponent::class.java)?.setScaleVector(Vector3.ONE*frame.scale)
        root?.components?.get(OpacityControllerComponent::class.java)?.opacity=frame.opacity
    }
    private fun finishEntrance(s:RoseSession) {
        val anchor=root ?: return
        registerStemAccessibility(content,checkNotNull(stem))
        for(p in petals.values) {
            p.meshOwner.components.set(CollisionComponent(listOf(p.shape),PhysicsMaterialResource()))
            p.meshOwner.components.set(InteractableComponent())
            p.meshOwner.components.set(HoverEffectComponent())
            registerPetalAccessibility(content,p.owner,p.spec.id,s.id)
        }
        pinchDrag.clear()
        s.ready();publish()
        recentColors.recordReady(s.config.colorId)
        Log.i(TAG,"RoseEntranceFinished session=${s.id} scale=1 opacity=1; interactions enabled")
        // Keep the audio failure boundary outside model assembly and entrance readiness.
        audioPreparationJob=sceneScope.launch {
            prepareOptionalAudio(
                prepare={petalAudio.prepare(anchor) {
                    frameGate.awaitNextFrame()
                    if(disposed || s.id!=generation)throw CancellationException("Audio round no longer active")
                }},
                onFailure={Log.e(TAG,"PetalAudioPreparationFailed session=${s.id}; flower remains playable",it)},
            )
            prepareOptionalAudio(
                prepare={endingPetalRain.prepare(s.id,s.config)},
                onFailure={Log.e(TAG,"PetalRainPreparationFailed session=${s.id}; flower remains playable",it)},
            )
            prepareOptionalAudio(
                prepare={endingMusic.prepare()},
                onFailure={Log.e(TAG,"EndingMusicPreparationFailed session=${s.id}; flower remains playable",it)},
            )
        }
    }
    private fun registerStemAccessibility(content:SpatialViewContent,base:Entity) {
        base.components.set(AccessibilityComponent("月季花芯与短茎","完整，花瓣摘尽后仍保留"))
        content.setAccessibilityActions(base,"rose.stem") {}
    }
    private fun registerPetalAccessibility(content:SpatialViewContent,owner:Entity,id:Int,token:Long) {
        owner.components.set(AccessibilityComponent("月季花瓣","尚未摘取"))
        content.setAccessibilityActions(owner,"rose.petal.$id") {
            onClick(label="摘取花瓣") {accessiblePluck(token,id)}
        }
    }
    fun isPetalTarget(entity:Entity):Boolean {
        // SDK 6.2.8 applies this rule on down, then caches that entity for move/up.
        val s=session ?: return false
        if(disposed || busy || appPaused || s.phase !in listOf(Phase.Ready,Phase.Playing))return false
        return petals.values.any {it.meshOwner==entity && it.meshOwner.valid && s.state(it.spec.id)==PetalState.Attached}
    }
    fun pointer(events: List<SpatialPointerInfo>): Boolean {
        val s=session ?: return false
        if(disposed || busy || appPaused || s.phase !in listOf(Phase.Ready,Phase.Playing,Phase.FinalPetalHeld))return false
        var handled=false
        for(e in events) {
            val hand=e.pointerId.value
            val kind=when(e.kind) {
                InteractionKind.DirectPinch->PinchDragTracker.Kind.Direct
                InteractionKind.GazePinch,InteractionKind.RayBasedPinch,InteractionKind.Pointer->PinchDragTracker.Kind.Remote
                else->PinchDragTracker.Kind.Other
            }
            val target=if(e.isDownEvent())petals.values.firstOrNull {
                it.meshOwner==e.targetedEntity && it.meshOwner.valid && s.state(it.spec.id)==PetalState.Attached
            }?.spec?.id else null
            val step=pinchDrag.update(PinchDragTracker.Sample(hand,e.isDownEvent(),e.isUpEvent(),e.pressed,kind,target),
                position={source->pointerPosition(e,source)},
                displacement=::pointerDisplacement,
                grab={id,pointer->petals[id]?.meshOwner?.valid==true && s.grab(s.id,id,pointer)})
            when(step) {
                is PinchDragTracker.Step.Started -> {
                    val p=petals.getValue(step.petal)
                    handled=true
                    stemMotion.grab()
                    if(p.returning>=0f){p.returning=-1f;p.owner.components.set(TransformComponent(p.rest))}
                    petalDrag.clear();remotePetalDrag.clear();heldPetalRotation.clear();lostTime=-1f;trackingUnavailable=false
                    if(step.source==PinchDragTracker.PositionSource.GlobalDevicePose)remotePetalDrag.start()
                    p.owner.components[AccessibilityComponent::class.java]?.value="正在拉取"
                    Log.i(TAG,"PetalGrab session=${s.id} petal=${p.spec.id} kind=${e.kind} source=${step.source} pointer=$hand")
                    gazeTrace {"grab pointer=$hand source=${step.source} raw=${e.inputDevicePose.rawPosition} local=${e.position3D}"}
                }
                is PinchDragTracker.Step.Anchored -> {
                    handled=true;lostTime=-1f;trackingUnavailable=false
                    gazeTrace {"anchor pointer=$hand downRaw=${step.downPosition} moveRaw=${step.position} appliedDelta=0"}
                }
                is PinchDragTracker.Step.Moved -> {
                    handled=true;lostTime=-1f;trackingUnavailable=false
                    // Only the captured pointer reaches this branch, even when kind/hit changes.
                    // Observe before detach so the crossing sample is the rotation baseline.
                    heldPetalRotation.observe(pointerRotation(e))
                    if(remotePetalDrag.active) {
                        remotePetalDrag.moveBy(step.frameDelta)
                        gazeTrace {"queued pointer=$hand delta=${step.frameDelta} total=${step.delta}"}
                        // Match DirectPinch's input phase. In SceneEvents.Update, transform setters
                        // are batched but convertTransformTo reads native state before that flush.
                        applyRemotePetalDrag()
                    } else {
                        // Keep DirectPinch's existing absolute-delta path unchanged.
                        petalDrag.move(step.delta);pull(hand)
                    }
                    applyHeldRotation()
                }
                is PinchDragTracker.Step.Unavailable -> {
                    handled=true;if(lostTime<0)lostTime=0f
                    heldPetalRotation.observe(pointerRotation(e));applyHeldRotation()
                }
                is PinchDragTracker.Step.Released -> {handled=true;release(hand,step.cancelled)}
                null -> Unit
            }
        }
        return handled
    }
    private fun pointerRotation(e:SpatialPointerInfo):Quat? = runCatching {
        val pose=e.inputDevicePose
        val p=pose.rawPosition
        // SDK uses a zero position + identity rotation when no device pose is supplied.
        // A valid identity orientation at a nonzero position must remain usable.
        if(!p.x.isFinite() || !p.y.isFinite() || !p.z.isFinite() ||
            (p.x==0f && p.y==0f && p.z==0f))return@runCatching null
        val raw=pose.rawRotation.toQuaternion()
        if(!raw.isFinite())return@runCatching null
        // Device orientation is consumed independently of the view-position conversion.
        // convertRotation negates X/Z; remove it following the headset report of reversed axes.
        // RoseAnchor has translation/uniform scale only; retain the device quaternion axes.
        gazeTrace {"handRotation revision=device-rotation-v2 pointer=${e.pointerId.value} kind=${e.kind} raw=$raw"}
        raw.normalize()
    }.getOrNull()
    private fun applyHeldRotation() {
        val s=session ?: return
        val p=petals[s.heldId] ?: return
        if(s.state(p.spec.id)!=PetalState.DetachedHeld)return
        val rotation=heldPetalRotation.rotation ?: return
        p.owner.components[TransformComponent::class.java]!!.setQuaternion(rotation)
        gazeTrace {"rotation revision=device-rotation-v2 petal=${p.spec.id} remote=${remotePetalDrag.active} rotation=$rotation"}
    }
    private fun pointerPosition(e:SpatialPointerInfo,source:PinchDragTracker.PositionSource):Vec? {
        val direct=source==PinchDragTracker.PositionSource.LocalPointer
        val p=if(direct)e.position3D else e.inputDevicePose.rawPosition
        if(!p.x.isFinite() || !p.y.isFinite() || !p.z.isFinite())return null
        // A zero local pointer is a valid point. The remote identity device pose is unavailable.
        if(!direct && p.x==0f && p.y==0f && p.z==0f)return null
        // Remote capture keeps the raw device origin. Map its difference as a vector below:
        // subtracting separately converted global points can introduce changing view-origin offsets.
        if(!direct)return Vec(p.x,p.y,p.z)
        // Keep DirectPinch's working local-pointer path unchanged.
        val v=content.convertPosition(Vector3(p.x,p.y,p.z),ViewCoordinateSpace.Local,content.localSpatialCoordinateSpace)
        return Vec(v.x,v.y,v.z)
    }
    private fun pointerDisplacement(source:PinchDragTracker.PositionSource,origin:Vec,current:Vec):Vec {
        val delta=current-origin
        if(source==PinchDragTracker.PositionSource.LocalPointer) {
            gazeTrace {"direct origin=$origin current=$current total=$delta"}
            return delta
        }
        // convertSize scales pixels to scene meters without adding the view's position.
        // Device/view axes have Y down; the scene and RoseAnchor have Y up.
        val v=content.convertSize(Vector3(delta.x,-delta.y,delta.z),ViewCoordinateSpace.Global,content.localSpatialCoordinateSpace)
        gazeTrace {"sample rawPrevious=$origin rawCurrent=$current rawDelta=$delta sceneDelta=${Vec(v.x,v.y,v.z)}"}
        return Vec(v.x,v.y,v.z)
    }
    private fun pull(hand: Long) {
        val s=session ?: return;val p=petals[s.heldId] ?: return
        val delta=petalDrag.delta
        if(s.state(p.spec.id)==PetalState.DetachedHeld) {
            val position=petalDrag.detachedPosition ?: return
            p.owner.components[TransformComponent::class.java]!!.setPosition(position.vector())
            return
        }
        applyPullPose(p,delta)
        if(PullRules.distance(delta)>=PullRules.THRESHOLD && detach(s.id,p,hand)!=null) {
            stemMotion.release(rebound=true)
        }
    }
    private fun applyPullPose(p:PetalVisual,delta:Vec) {
        val strength=PullRules.strength(PullRules.distance(delta))
        applyBend(stemMotion.pull(delta,strength))
        p.owner.components[TransformComponent::class.java]!!.apply {
            setPosition(p.spec.position.vector()+crown!!.convertPositionFrom(delta.vector()+crown!!.convertPositionTo(Vector3.ZERO,root),root))
            setQuaternion(petalRotation(p.spec,strength*7f))
        }
    }
    private fun detach(token:Long,p:PetalVisual,hand:Long):Transform? {
        val s=session ?: return null
        if(!s.detach(token,p.spec.id,hand))return null
        // The seated root, in RoseAnchor meters, includes the current loaded stem bend.
        // Sample before reparenting or rebound; the pulled petal itself is already displaced.
        val tearPosition=crown!!.convertPositionTo(p.spec.position.vector(),root)
        petalAudio.play(p.spec.id,tearPosition)
        // Preserve the petal's pose before it stops inheriting the stem motion.
        val world=p.owner.convertTransformTo(Transform(),root)
        p.owner.removeFromParent();root!!.addChild(p.owner);p.owner.components.set(TransformComponent(world))
        petalDrag.detach(Vec(world.position.x,world.position.y,world.position.z))
        heldPetalRotation.detach(world.quaternion)
        if(Log.isLoggable(GAZE_TAG,Log.DEBUG) || remotePetalDrag.active) {
            val after=p.owner.convertTransformTo(Transform(),root)
            Log.i(GAZE_TAG,"revision=move-anchor-v4 stage=reparent remote=${remotePetalDrag.active} session=$token petal=${p.spec.id} " +
                "pull=${remotePetalDrag.displacement} before=$world after=$after")
        }
        // Keep the same native input owner/collider for the captured pointer until release.
        // RoseSession and the down-target predicate reject every attempt to grab it again.
        p.meshOwner.components.remove(HoverEffectComponent::class.java)
        content.clearAccessibilityActions(p.owner);p.owner.components[AccessibilityComponent::class.java]?.value="已经摘下，等待松手"
        publish();Log.i(TAG,"PetalDetached session=$token petal=${p.spec.id} k=${s.detachedCount} result=${s.result}")
        return world
    }
    private fun release(hand:Long,immediate:Boolean) {
        val s=session ?: return;val p=petals[s.heldId] ?: return
        // Preserve ordered draining before capture cleanup and fallStart, including cancellation.
        applyRemotePetalDrag()
        applyHeldRotation()
        pinchDrag.clear()
        when(s.release(s.id,hand)) {
            PetalState.Falling -> {
                p.meshOwner.components.remove(InteractableComponent::class.java)
                p.meshOwner.components.remove(CollisionComponent::class.java)
                p.fallStart=p.owner.convertTransformTo(Transform(),root);p.falling=0f
                if(remotePetalDrag.active)gazeTrace {"release petal=${p.spec.id} fallStart=${p.fallStart}"}
                // Opaque while attached/held. Only the released petal changes blend mode.
                p.material.setBlendingMode(BlendingMode.TRANSPARENT)
                p.owner.components[AccessibilityComponent::class.java]?.isAccessibilityElement=false
            }
            PetalState.Attached -> {
                p.returnStart=p.owner.components[TransformComponent::class.java]!!.getLocalMatrix().toTransform();p.returning=0f
                p.owner.components[AccessibilityComponent::class.java]?.value="尚未摘取"
                if(!immediate)stemMotion.release(rebound=false)
            }
            else -> Unit
        }
        if(immediate)applyBend(stemMotion.reset())
        lostTime=-1f;petalDrag.clear();remotePetalDrag.clear();heldPetalRotation.clear();publish()
    }
    private fun applyRemotePetalDrag() {
        val s=session ?: return
        val p=petals[s.heldId] ?: return
        if(!remotePetalDrag.active)return
        val position=remotePetalDrag.advance {delta,crossing->
            val before=if(Log.isLoggable(GAZE_TAG,Log.DEBUG))p.owner.convertTransformTo(Transform(),root) else null
            applyPullPose(p,delta)
            gazeTrace {"pull petal=${p.spec.id} delta=$delta strength=${PullRules.strength(delta.length())} " +
                "crossing=$crossing before=$before loaded=${p.owner.convertTransformTo(Transform(),root)}"}
            if(crossing) {
                val world=checkNotNull(detach(s.id,p,checkNotNull(s.handId)))
                stemMotion.release(rebound=true)
                // Seed from exactly the same snapshot used for reparenting, without a native re-read.
                val atTear=world.position
                Vec(atTear.x,atTear.y,atTear.z)
            } else null
        } ?: return
        p.owner.components[TransformComponent::class.java]!!.setPosition(position.vector())
        gazeTrace {"held petal=${p.spec.id} position=$position"}
    }
    fun streamLost() {pinchDrag.clear();if(session?.heldId!=null && lostTime<0)lostTime=0f}
    private fun accessiblePluck(token:Long,id:Int):Boolean {
        val s=session ?: return false
        if(disposed || busy || appPaused || s.phase !in listOf(Phase.Ready,Phase.Playing))return false
        if(!s.grab(token,id,-1))return false
        if(detach(token,petals.getValue(id),-1)==null)return false
        release(-1,false);return true
    }
    private fun applyBend(v:Vec) {
        val pose=skeleton ?: return
        if(restPose.size!=3)return
        val axis=Vector3(v.z,0f,-v.x)
        val angle=v.length()*8f*PI.toFloat()/180
        val q=if(axis.length()<.001f)Quat.identity() else Quat(axis,angle*.5f)
        val transforms=restPose.mapIndexed {i,t->Transform(t.position,if(i==midIndex || i==tipIndex)t.quaternion*q else t.quaternion,t.scale)}
        check(pose.setJointTransforms(transforms)) {"Stem pose update rejected"}
        // Use the authored GLB bind vectors: SDK pose-position space is not established here.
        // Preserve the existing rotational bend convention and share its result with the core.
        val mid=restPose[midIndex].quaternion*q
        val tip=mid*(restPose[tipIndex].quaternion*q)
        val head=Vector3(0f,.04f,0f)+RoseStem.root.vector()+RoseStem.midLink.vector()+mid.rotateVector(RoseStem.tipLink.vector())
        crown?.components?.get(TransformComponent::class.java)?.apply {setPosition(head);setQuaternion(tip*EulerAngles(RoseLayout.HEAD_PITCH,0f,0f).toQuat())}
    }
    private fun tick(dt:Float) {
        val s=session ?: return
        if(appPaused || busy || disposed || s.phase==Phase.Error)return
        if(petals.values.any { !it.owner.valid && s.state(it.spec.id)!=PetalState.Removed }) {s.fail();publish();return}
        if(s.phase==Phase.Loading) {
            if(entranceAwaitingFrame) {
                // Commit full scale to the scene before attaching exact mesh colliders.
                entranceAwaitingFrame=false
                finishEntrance(s)
            } else entrance.advance(dt)?.let {frame->
                applyEntrance(frame)
                if(frame.finished)entranceAwaitingFrame=true
            }
            return
        }
        if(lostTime>=0f){lostTime+=dt;if(lostTime>=.25f) {trackingUnavailable=true;s.handId?.let {release(it,true)}}}
        stemMotion.advance(dt)?.let(::applyBend)
        for(p in petals.values) {
            if(p.returning>=0f) {
                p.returning+=dt;val t=(p.returning/StemMotion.CANCEL_TIME).coerceIn(0f,1f);val a=p.returnStart!!;val b=p.rest
                p.owner.components.set(TransformComponent(Transform(a.position*(1-t)+b.position*t,Quat.slerp(a.quaternion,b.quaternion,t),b.scale)))
                if(t>=1)p.returning=-1f
            }
            if(p.falling>=0f) {
                p.falling+=dt;val t=(p.falling/1.2f).coerceIn(0f,1f);val a=p.fallStart!!
                val side=sin(t*PI.toFloat())*.025f*(if(p.spec.id%2==0)1 else -1)
                p.owner.components[TransformComponent::class.java]!!.apply {
                    setPosition(a.position+Vector3(side,-.24f*t,0f))
                    setQuaternion(a.quaternion*Quat(Vector3(1f,.3f,.2f),t*.75f))
                }
                p.material.setOpacity(1-t*t*(3-2*t))
                if(t>=1f){p.owner.destroy();p.falling=-1f;s.removed(p.spec.id)}
            }
        }
        if(s.phase in listOf(Phase.Completing,Phase.Completed)) {
            glowTime=(glowTime+dt).coerceAtMost(1.2f)
            val light=1f+.14f*sin(glowTime/1.2f*PI.toFloat())
            for((mat,color) in coreMaterials)mat.setBaseColor(Color4((color.red*light).coerceAtMost(1f),(color.green*light).coerceAtMost(1f),(color.blue*light).coerceAtMost(1f),1f))
        }
        publish()
    }
    fun cancelInput() {
        session?.handId?.let {release(it,true)}
        pinchDrag.clear()
    }
    fun pause() {
        appPaused=true
        cancelInput()
        petalAudio.stop()
        endingPetalRain.stop()
        endingMusic.stop()
        applyBend(stemMotion.reset())
        appPaused=true;session?.pause()
    }
    fun resume(){appPaused=false;session?.resume()}
    private fun publish() {
        val s=session ?: return
        updateUi(RoseUi(s.phase,s.result,s.detachedCount==0,trackingUnavailable,s.blessing,s.id))
    }
    private fun updateUi(state:RoseUi) {
        if(state!=ui){
            val blessingAppeared = state.phase==Phase.Completing &&
                (ui.roundId!=state.roundId || ui.phase!=Phase.Completing)
            ui=state
            syncPanels(state)
            if(blessingAppeared && !appPaused)endingMusic.blessing(state.roundId)
        }
    }
    /** Acknowledges the exact blank draw, including the outgoing round's erase. */
    fun completionFrameCommitted(frame:CompletionBufferGate.Request) {
        if(disposed || !completionBuffer.committed(frame))return
        syncPanels(ui)
        Log.i(TAG,"CompletionFrameCommitted round=${frame.revealRound} serial=${frame.serial} " +
            "stage=${if(frame.revealRound==null) "erased" else "reveal"} revision=buffer-gate-v2")
    }
    private fun syncPanels(state:RoseUi) {
        resultPanel?.components?.get(AccessibilityComponent::class.java)?.value=state.result.ifEmpty{"尚未摘取"}
        messagePanel?.components?.get(AccessibilityComponent::class.java)?.value=when {
            state.phase==Phase.Loading->"花正在盛开…"
            state.phase==Phase.Error->"这朵花暂时没准备好，请再试一次。重新开始可用"
            state.phase==Phase.Completed || state.phase==Phase.Completing->""
            state.trackingLost->"暂时没看清你的手，放松一下，再试试。"
            state.teaching->"看向或靠近一片花瓣，捏住，轻轻拉开。"
            else->"继续摘取"
        }
        val completing=state.phase==Phase.Completing || state.phase==Phase.Completed
        completionBuffer.update(state.roundId,completing)
        completionPresentedRound=completionBuffer.presentedRound
        completionBlankRequest=completionBuffer.request
        val presented=completing && completionPresentedRound==state.roundId
        completionPanel?.components?.get(AccessibilityComponent::class.java)?.value=if(presented)state.blessing else ""
        // Mask before enabling. On replay, keep drawing ONLY until the panel's own blank
        // buffer commits; disabling immediately would preserve the old opaque text/button.
        completionPanel?.components?.get(OpacityControllerComponent::class.java)?.opacity=if(presented)1f else 0f
        // Hide the outgoing surface synchronously, before its Compose buffer catches up.
        // Inactive panels must not leave transparent hit areas over the flower or replay button.
        completionPanel?.enabled=completionBuffer.enabled
        messagePanel?.enabled=!completing
        resultPanel?.enabled=state.result.isNotEmpty()
    }
    private fun clearRound() {
        audioPreparationJob?.cancel();audioPreparationJob=null
        entrance.clear()
        entranceAwaitingFrame=false
        petalAudio.clearRound()
        endingPetalRain.stop()
        // EndingMusic is scene-owned: its 1 s replay fade survives flower teardown/loading.
        skeleton=null;restPose=emptyList();stem=null;crown=null;stemMotion.reset();lostTime=-1f;trackingUnavailable=false;glowTime=0f;coreMaterials.clear()
        petalDrag.clear();remotePetalDrag.clear();heldPetalRotation.clear();pinchDrag.clear()
        root?.let {if(it.valid)it.destroy()};root=null
        for(p in petals.values){p.material.close();p.shape.close()};petals.clear()
    }
    fun close() {
        if(disposed)return
        disposed=true;generation++;initializationJob?.cancel();assemblyJob?.cancel()
        subscription?.cancel();subscription=null;clearRound()
        petalAudio.close()
        endingMusic.close()
        endingPetalRain.close()
        resultBinding.clear();messageBinding.clear();completionBinding.clear()
        completionBuffer.reset()
        completionBlankRequest=null
        completionPresentedRound=null
        resultPanel=null;messagePanel=null;completionPanel=null
        prototypes.values.forEach {if(it.valid)it.destroy()};prototypes.clear();textures.close()
    }
    private fun walk(entity:Entity):Sequence<Entity> = sequence {yield(entity);for(c in entity.getChildren())yieldAll(walk(c))}
    private fun Vec.vector()=Vector3(x,y,z)
    private inline fun gazeTrace(message:()->String) {
        if(Log.isLoggable(GAZE_TAG,Log.DEBUG))Log.d(GAZE_TAG,"revision=move-anchor-v4 session=${session?.id} ${message()}")
    }
    companion object {
        private const val TAG="FlowerTears"
        private const val GAZE_TAG="RoseGaze"
    }
}
