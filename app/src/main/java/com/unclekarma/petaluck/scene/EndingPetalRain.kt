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
import com.pico.spatial.core.ecs.particle.ParticleEmitterShape
import com.pico.spatial.core.ecs.particle.ParticleEmitterShapeType
import com.pico.spatial.core.ecs.particle.ParticleEmissionLocation
import com.pico.spatial.core.ecs.particle.ParticleEmissionSpace
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

/** Optional scene-owned bundle; each round gets a fresh instance so stop leaves no live petals. */
internal class EndingPetalRain(context: Context) : AutoCloseable {
    private val assets = context.applicationContext.assets
    private val loadingMutex = Mutex()
    private val textures = RoundAssetSet<TextureResource>({ it.valid }, { it.toGlobal() }, { it.close() })
    private var bundle: AssetBundle? = null
    private var instance: Entity? = null
    private var particles = emptyList<ParticleComponent>()
    private var finaleEmissionRandom: Random? = null
    private var tailDurationMs = AUTHORED_LIFETIME_MS
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
                    // Keep the owners alive: already-born petals finish fading out.
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
                Log.i(TAG, "PetalRainTailFinished round=$roundId tailMs=$tailDurationMs")
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
                    val paths = RoseLayout.rainTexturePaths(config)
                    if (surrounding) check(paths.size == RoseLayout.BASE_COLOR_COUNT)
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
                    // Independent from gameplay, palette and breeze sampling.
                    val emissionRandom = if (surrounding) Random(config.seed xor 0x454d49544cL) else null
                    val finaleRate = emissionRandom?.nextInt(120, 161)?.toFloat() ?: 0f
                    // A fixed sampled lifetime per emitter keeps normalized alpha keys at exactly
                    // one second for every particle it creates, without adding more emitters.
                    val lifetimeRandom = Random(config.seed xor 0x4c494645L)
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
                    var longestLifetimeMs = AUTHORED_LIFETIME_MS
                    val controls = owners.mapIndexed { index, owner ->
                        // Authored emitter is already near the +0.35 m container ceiling.
                        // Each color covers the full overhead disk at the Stage entry origin.
                        val position = if(surrounding) Vector3(0f, 2.15f + (index % 3) * .15f, 0f)
                            else Vector3(0f, .33f, 0f)
                        checkNotNull(owner.components[TransformComponent::class.java]).setPosition(position)
                        checkNotNull(owner.components[ParticleComponent::class.java]).also {
                            it.isEmitting = false
                            val copies = owners.indices.count { candidateIndex -> candidateIndex % 3 == index % 3 }
                            val rate = rates[index % 3]
                            if (surrounding) {
                                val lifetimeSeconds = lifetimeRandom.nextInt(7_000, 9_001) * 1.625f / 1_000f
                                it.configureFinaleEmitter(finaleRate, lifetimeSeconds)
                                Log.i(TAG, "FinaleEmitterLifetime index=$index seconds=$lifetimeSeconds fadeIn=1 fadeOut=1")
                            } else {
                                it.birthRateFull = ParticleFloatVaryingProperty(rate.type, rate.value / copies, rate.range / copies)
                                if (config.colorId in 0 until RoseLayout.BASE_COLOR_COUNT) {
                                    val lifetimeMs = AUTHORED_LIFETIME_MS + lifetimeRandom.nextInt(1_500, 2_001)
                                    it.mainSimulation.setRainLifetime(lifetimeMs / 1_000f)
                                    longestLifetimeMs = maxOf(longestLifetimeMs, lifetimeMs)
                                }
                            }
                            it.mainSimulation.apply {
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
                    finaleEmissionRandom = emissionRandom
                    tailDurationMs = longestLifetimeMs
                    preparedRound = roundId
                    retained = true
                    Log.i(TAG, "PetalRainReady round=$roundId color=${config.colorId} emitters=${controls.size} surrounding=$surrounding initialFinaleRate=$finaleRate revision=random-rate-v5 textures=$paths")
                } finally {
                    if (!retained && candidate.valid) candidate.destroy()
                }
            }
        } finally {
            loading = false
            if (closed) releaseBundle()
        }
    }

    fun start(roundId: Long, content: SpatialViewContent, endless: Boolean = false): Boolean {
        if (closed || preparedRound != roundId) {
            Log.w(TAG, "PetalRainNotReady round=$roundId")
            return false
        }
        return try {
            val owner = checkNotNull(instance)
            particles.forEach { it.isEmitting = true }
            // Direct SpatialView child: independent of RoseAnchor entrance, bend and teardown.
            content.addEntity(owner)
            owner.enabled = true
            if (!endless) playback.start(roundId, tailDurationMs)
            Log.i(TAG, "PetalRainStarted round=$roundId")
            true
        } catch (e: Exception) {
            stop()
            Log.e(TAG, "Petal rain start failed; music and flower remain playable", e)
            false
        }
    }

    /** Called once per second by the Stage's composition-owned playback coroutine. */
    fun randomizeFinaleEmissionRate(): Boolean {
        val random = finaleEmissionRandom ?: return false
        if (closed || particles.isEmpty()) return false
        return try {
            val total = random.nextInt(120, 161).toFloat()
            particles.forEach { it.setFinaleEmissionRate(total) }
            Log.d(TAG, "FinaleEmissionRate total=$total emitters=${particles.size}")
            true
        } catch (e: Exception) {
            stop()
            Log.e(TAG, "Finale emission update failed", e)
            false
        }
    }

    fun stop() {
        epoch++
        playback.cancel()
        val previous = instance
        val previousRound = preparedRound
        instance = null
        particles = emptyList()
        finaleEmissionRandom = null
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
        private const val AUTHORED_LIFETIME_MS = 5_000L
        private const val SCENE_KEY = "Scenes/PetalRain"
        private val EMITTERS = listOf("PetalRain5", "PetalRain6", "PetalRain7")
    }
}

/** Apply a shared total rate without resetting live particles, shape or animation. */
internal fun ParticleComponent.setFinaleEmissionRate(totalBirthRate: Float) {
    require(totalBirthRate in 120f..160f)
    val rate = totalBirthRate / RoseLayout.BASE_COLOR_COUNT
    birthRateFull = ParticleFloatVaryingProperty(ParticleVaryingPropertyType.CONSTANT, rate, rate)
}

/** Eight overlapping color emitters share the Stage's sampled total emission rate. */
internal fun ParticleComponent.configureFinaleEmitter(totalBirthRate: Float, lifetimeSeconds: Float) {
    setFinaleEmissionRate(totalBirthRate)
    val authored = emitterShape
    emitterShape = ParticleEmitterShape(
        type = ParticleEmitterShapeType.CYLINDER,
        // Shrink both horizontal dimensions by 25%.
        size = Vector3(7.05f, .001f, 7.05f),
        emissionLocation = ParticleEmissionLocation.VOLUME,
        vertexCount = authored.vertexCount,
        emissionSpace = ParticleEmissionSpace.LOCAL,
        emissionDirection = Vector3(0f, -1f, 0f),
        radialSpan = 360f,
        torusInnerRadius = authored.torusInnerRadius,
    )
    mainSimulation.configureFinaleRain(lifetimeSeconds)
}

/** Applied once to each attached, freshly cloned Stage emitter. */
internal fun ParticleSimulation.configureFinaleRain(lifetimeSeconds: Float) {
    require(lifetimeSeconds in 11.375f..14.625f)
    maxParticleCount = 320 // Up to ceil(20 births/s × 14.625 s) = 293 live petals per color.
    startSize = startSize.let { ParticleFloatVaryingProperty(it.type, it.value * 2f, it.range * 2f) }
    // Authored motion has no gravity/drag module; falling is driven by downward startSpeed.
    // Sample per particle at birth: a wider spread with almost the same mean falling speed.
    startSpeed = ParticleFloatVaryingProperty(ParticleVaryingPropertyType.RANDOM, .05f, .20f)
    isRotationVelocityCurveEnabled = true
    // Signed, per-particle XYZ sampling mixes slow turns and fast flips in both directions.
    // Mean absolute speeds remain close to v5's 35.6 / 24.4 / 30 degrees per second.
    listOf(rotationVelocityCurveX to 72f, rotationVelocityCurveY to 50f,
        rotationVelocityCurveZ to 60f).forEach { (curve, limit) ->
        curve.curveType = ParticleCurveType.RANDOM
        curve.multiplier = 1f
        curve.start = -limit
        curve.end = limit
    }
    setRainLifetime(lifetimeSeconds)
}

private fun ParticleSimulation.setRainLifetime(lifetimeSeconds: Float) {
    lifetime = ParticleFloatVaryingProperty(ParticleVaryingPropertyType.CONSTANT, lifetimeSeconds, lifetimeSeconds)
    // Per-emitter sampled lifetime: 1s fade in + (lifetime - 2)s opaque + 1s fade out.
    // The bundled component is attached already; these immediate native key writes require it.
    clearColorGradientKeys()
    addColorGradientColorKey(Color3(1f, 1f, 1f), 0f)
    addColorGradientColorKey(Color3(1f, 1f, 1f), 1f)
    addColorGradientAlphaKey(0f, 0f)
    addColorGradientAlphaKey(1f, 1f / lifetimeSeconds)
    addColorGradientAlphaKey(1f, 1f - 1f / lifetimeSeconds)
    addColorGradientAlphaKey(0f, 1f)
}
