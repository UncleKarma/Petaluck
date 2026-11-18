package com.unclekarma.petaluck.scene

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.unclekarma.petaluck.domain.RoundConfig
import com.unclekarma.petaluck.domain.RoseLayout
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ParticleComponent
import com.pico.spatial.core.ecs.particle.ParticleFloatVaryingProperty
import com.pico.spatial.core.ecs.particle.ParticleCurveType
import com.pico.spatial.core.ecs.particle.ParticleSimulation
import com.pico.spatial.core.ecs.particle.ParticleVaryingPropertyType
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.AssetBundle
import com.pico.spatial.core.ecs.resource.TextureCreateOption
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.core.math.Color3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/** Optional scene-owned bundle; each round gets a fresh instance so stop leaves no live petals. */
internal class EndingPetalRain(context: Context) : AutoCloseable {
    private val assets = context.applicationContext.assets
    private val loadingMutex = Mutex()
    private val textures = RoundAssetSet<TextureResource>({ it.valid }, { it.toGlobal() }, { it.close() })
    private var bundle: AssetBundle? = null
    private var instance: Entity? = null
    private var particles = emptyList<ParticleComponent>()
    private var preparedRound: Long? = null
    private var epoch = 0L
    private var loading = false
    private var closed = false
    private val handler = Handler(Looper.getMainLooper())
    private val playback = PetalRainSchedule(
        schedule = { delay, action -> handler.postDelayed({ action() }, delay); Unit },
        cancelScheduled = { handler.removeCallbacksAndMessages(null) },
        onDrain = { roundId ->
            if (!closed && preparedRound == roundId) {
                try {
                    // Keep the owners alive: already-born petals finish their five-second life.
                    particles.forEach { it.isEmitting = false }
                    Log.i(TAG, "PetalRainEmissionEnded round=$roundId elapsedMs=10000")
                } catch (e: Exception) {
                    stop()
                    Log.e(TAG, "Petal rain emission stop failed", e)
                }
            }
        },
        onFinished = { roundId ->
            if (preparedRound == roundId) {
                Log.i(TAG, "PetalRainTailFinished round=$roundId elapsedMs=15000")
                stop()
            }
        },
    )

    suspend fun prepare(roundId: Long, config: RoundConfig, surrounding: Boolean = false) = loadingMutex.withLock {
        if (closed || preparedRound == roundId) return@withLock
        stop()
        val token = epoch
        val caller = currentCoroutineContext()
        loading = true
        // Native suspend loads do not expose cancellation ownership. Always collect their result
        // on main and destroy stale entities, including a result arriving after scene disposal.
        try {
            withContext(NonCancellable) {
                val manifest = withContext(Dispatchers.IO) {
                    assets.open("petal-rain.scenes.json").bufferedReader().use { JSONObject(it.readText()) }
                }
                check(manifest.getString("schemaVersion") == "0.1")
                check(manifest.getString("scope") == "bundle_scenes")
                val scenes = manifest.getJSONArray("scenes")
                check(scenes.length() == 1)
                val scene = scenes.getJSONObject(0)
                check(scene.getString("sourcePath") == manifest.getString("activeScenePath"))
                val key = scene.getString("runtimeSceneKey")
                check(key == SCENE_KEY)
                val roots = scene.getJSONArray("entities")
                val names = (0 until roots.length()).map { index ->
                    val entry = roots.getJSONObject(index)
                    check(entry.getString("parentPath") == "Root")
                    val name = entry.getString("name")
                    check(entry.getString("path") == "Root/$name")
                    name
                }
                check(scene.getInt("rootCount") == 3 && names.sorted() == EMITTERS.sorted())
                val source = bundle ?: withContext(Dispatchers.IO) {
                    AssetBundle.load("asset://petal-rain.bundle")
                }.also { bundle = it }
                if (closed || token != epoch || !caller.isActive) return@withContext
                val candidate = source.loadModelSuspend(key)
                var retained = false
                try {
                    candidate.enabled = false
                    if (closed || token != epoch || !caller.isActive) return@withContext
                    // Keep the SDK return distinct from the manifest's Root scope. Fail closed
                    // for an unknown wrapper layout rather than recursively matching names.
                    val scope = candidate.getChildren().single()
                    val children = scope.getChildren()
                    check(children.map { it.getName() }.sorted() == names.sorted()) {
                        "Petal rain manifest/runtime scope mismatch"
                    }
                    val colors = RoseLayout.rainTexturePaths(config)
                    val paths = if(surrounding) List(24) { colors[it % colors.size] } else colors
                    textures.retain(paths.toSet()) { path ->
                        withContext(Dispatchers.IO) {
                            val bitmap = PetalRainSprite.draw(assets, path)
                            try { TextureResource.create(bitmap, TextureCreateOption()) }
                            finally { bitmap.recycle() }
                        }
                    }
                    if (closed || token != epoch || !caller.isActive) return@withContext
                    // Own PRNG: decorative motion never consumes the gameplay/layout random stream.
                    val breeze = Random(config.seed xor 0x504554414CL)
                    val originals = EMITTERS.map { name -> children.single { it.getName() == name } }
                    val rates = originals.map { checkNotNull(it.components[ParticleComponent::class.java]).birthRateFull }
                    // Clone before editing any source material/rate. Every clone belongs to candidate
                    // immediately, so cancellation/failure destroys the entire prepared effect.
                    val owners = paths.indices.map { index ->
                        if (index < originals.size) originals[index] else {
                            val copy = checkNotNull(originals[index % 3].clone(
                                Entity.CloneOptions(recursive = true, shouldShareMaterialInstance = false)))
                            if (!scope.addChild(copy)) {
                                copy.destroy()
                                error("Cannot attach multicolor rain emitter")
                            }
                            copy
                        }
                    }
                    val controls = owners.mapIndexed { index, owner ->
                        // Authored emitter is already near the +0.35 m container ceiling.
                        val position = if(surrounding) {
                            val angle = (index % 8) * (2.0 * PI / 8.0) + (index / 8) * .23
                            // Three rings within the 5 m disk around the Stage user origin;
                            // leave room at the boundary for the authored spawn shape and drift.
                            val radius = .8 + (index / 8) * 1.8
                            Vector3((sin(angle) * radius).toFloat(), 2.15f + (index % 3) * .15f,
                                (cos(angle) * radius).toFloat())
                        } else Vector3(0f, .33f, 0f)
                        checkNotNull(owner.components[TransformComponent::class.java]).setPosition(position)
                        checkNotNull(owner.components[ParticleComponent::class.java]).also {
                            it.isEmitting = false
                            val copies = owners.indices.count { candidateIndex -> candidateIndex % 3 == index % 3 }
                            val rate = rates[index % 3]
                            // Finale was already 2x authored emission; this increment triples it.
                            val density = if(surrounding) 6f else 1f
                            it.birthRateFull = ParticleFloatVaryingProperty(rate.type, rate.value * density / copies, rate.range * density / copies)
                            it.mainSimulation.apply {
                                if(surrounding) configureFinaleRain()
                                material.baseColorMap = checkNotNull(textures[paths[index]])
                                noiseSeed = breeze.nextInt(1, Int.MAX_VALUE)
                                noiseStrength = .00045f + breeze.nextFloat() * .0003f
                                noiseScale = .12f + breeze.nextFloat() * .04f
                                noiseAnimationSpeed = .2f + breeze.nextFloat() * .1f
                            }
                        }
                    }
                    instance = candidate
                    particles = controls
                    preparedRound = roundId
                    retained = true
                    Log.i(TAG, "PetalRainReady round=$roundId color=${config.colorId} emitters=${controls.size} y=0.33 revision=multicolor-v1 textures=$paths")
                } finally {
                    if (!retained && candidate.valid) candidate.destroy()
                }
            }
        } finally {
            loading = false
            if (closed) releaseBundle()
        }
    }

    fun start(roundId: Long, content: SpatialViewContent, endless: Boolean = false) {
        if (closed || preparedRound != roundId) {
            Log.w(TAG, "PetalRainNotReady round=$roundId")
            return
        }
        try {
            val owner = checkNotNull(instance)
            particles.forEach { it.isEmitting = true }
            // Direct SpatialView child: independent of RoseAnchor entrance, bend and teardown.
            content.addEntity(owner)
            owner.enabled = true
            if (!endless) playback.start(roundId)
            Log.i(TAG, "PetalRainStarted round=$roundId")
        } catch (e: Exception) {
            stop()
            Log.e(TAG, "Petal rain start failed; music and flower remain playable", e)
        }
    }

    fun stop() {
        epoch++
        playback.cancel()
        val previous = instance
        val previousRound = preparedRound
        instance = null
        particles = emptyList()
        preparedRound = null
        try {
            // isEmitting=false alone leaves surviving particles; disable and destroy the instance.
            if (previous?.valid == true) {
                previous.enabled = false
                previous.destroy()
                Log.i(TAG, "PetalRainStopped round=$previousRound instanceDestroyed=true")
            }
        } catch (e: Exception) { Log.e(TAG, "Petal rain cleanup failed", e) }
    }

    override fun close() {
        if (closed) return
        closed = true
        stop()
        if (!loading) releaseBundle()
    }

    private fun releaseBundle() {
        val previous = bundle
        bundle = null
        try { previous?.releaseModel(SCENE_KEY) }
        catch (e: Exception) { Log.e(TAG, "Petal rain model release failed", e) }
        try { previous?.close() }
        catch (e: Exception) { Log.e(TAG, "Petal rain bundle release failed", e) }
        try { textures.close() }
        catch (e: Exception) { Log.e(TAG, "Petal rain texture release failed", e) }
    }

    companion object {
        private const val TAG = "Petaluck"
        private const val SCENE_KEY = "Scenes/PetalRain"
        private val EMITTERS = listOf("PetalRain5", "PetalRain6", "PetalRain7")
    }
}

/** Applied once to each attached, freshly cloned Stage emitter; volume rain keeps its asset values. */
internal fun ParticleSimulation.configureFinaleRain() {
    maxParticleCount *= 6 // Previous Stage capacity was 2x; now triple that capacity.
    startSize = startSize.let { ParticleFloatVaryingProperty(it.type, it.value * 2f, it.range * 2f) }
    // Authored motion has no gravity/drag module; falling is driven by downward startSpeed.
    startSpeed = startSpeed.let { ParticleFloatVaryingProperty(it.type, it.value * 1.5f, it.range * 1.5f) }
    lifetime = ParticleFloatVaryingProperty(ParticleVaryingPropertyType.CONSTANT, 4f, 4f)
    listOf(rotationVelocityCurveX, rotationVelocityCurveY, rotationVelocityCurveZ).forEach { curve ->
        // Scale the active representation, not both its values and multiplier.
        when (curve.curveType) {
            ParticleCurveType.CONSTANT -> curve.value *= 1.5f
            ParticleCurveType.RANDOM, ParticleCurveType.LINEAR -> {
                val start = curve.start
                val end = curve.end
                curve.start = start * 1.5f
                curve.end = end * 1.5f
            }
            ParticleCurveType.CURVE, ParticleCurveType.RANDOM_BETWEEN_TWO_CURVES -> curve.multiplier *= 1.5f
            ParticleCurveType.UNKNOWN -> error("Unsupported finale rotation velocity curve")
        }
    }
    // Normalized lifetime keys: 1s fade in + 2s opaque + 1s fade out.
    // The bundled component is attached already; these immediate native key writes require it.
    clearColorGradientKeys()
    addColorGradientColorKey(Color3(1f, 1f, 1f), 0f)
    addColorGradientColorKey(Color3(1f, 1f, 1f), 1f)
    addColorGradientAlphaKey(0f, 0f)
    addColorGradientAlphaKey(1f, .25f)
    addColorGradientAlphaKey(1f, .75f)
    addColorGradientAlphaKey(0f, 1f)
}
