package com.example.flowertears.p18ab52cf.scene

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.flowertears.p18ab52cf.domain.RoundConfig
import com.example.flowertears.p18ab52cf.domain.RoseLayout
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ParticleComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.AssetBundle
import com.pico.spatial.core.ecs.resource.TextureCreateOption
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.math.Vector3
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

    suspend fun prepare(roundId: Long, config: RoundConfig) = loadingMutex.withLock {
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
                    val paths = RoseLayout.layerNames.map { RoseLayout.texturePath(config.colorId, "${it}_01") }
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
                    val controls = EMITTERS.mapIndexed { index, name ->
                        val owner = children.single { it.getName() == name }
                        // Authored emitter is already near the +0.35 m container ceiling.
                        checkNotNull(owner.components[TransformComponent::class.java])
                            .setPosition(Vector3(0f, .33f, 0f))
                        checkNotNull(owner.components[ParticleComponent::class.java]).also {
                            it.isEmitting = false
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
                    preparedRound = roundId
                    retained = true
                    Log.i(TAG, "PetalRainReady round=$roundId color=${config.colorId} emitters=${controls.size} y=0.33 revision=drain-tail-v5 textures=$paths")
                } finally {
                    if (!retained && candidate.valid) candidate.destroy()
                }
            }
        } finally {
            loading = false
            if (closed) releaseBundle()
        }
    }

    fun start(roundId: Long, content: SpatialViewContent) {
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
            playback.start(roundId)
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
        private const val TAG = "FlowerTears"
        private const val SCENE_KEY = "Scenes/PetalRain"
        private val EMITTERS = listOf("PetalRain5", "PetalRain6", "PetalRain7")
    }
}
