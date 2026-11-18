package com.unclekarma.petaluck

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ParticleComponent
import com.pico.spatial.core.ecs.particle.ParticleCurveType
import com.pico.spatial.core.ecs.resource.AssetBundle
import com.unclekarma.petaluck.platform.LaunchActivity
import com.unclekarma.petaluck.scene.configureFinaleRain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the packaged bundle and native SDK, including clone isolation. Requires a PICO runtime. */
@RunWith(AndroidJUnit4::class)
class FinaleRainNativeTest {
    @Test fun attachedCloneRetainsFinaleParametersWithoutChangingVolumeTemplate() {
        ActivityScenario.launch(LaunchActivity::class.java).use {
            runBlocking(Dispatchers.Main) {
                val bundle = withContext(Dispatchers.IO) { AssetBundle.load("asset://petal-rain.bundle") }
                try {
                    val root = bundle.loadModelSuspend("Scenes/PetalRain")
                    try {
                        root.enabled = false
                        val scope = root.getChildren().single()
                        val originals = scope.getChildren()
                        assertEquals(3, originals.size)
                        for (original in originals) {
                            val source = checkNotNull(original.components[ParticleComponent::class.java]).mainSimulation
                            val size = source.startSize
                            val speed = source.startSpeed
                            val capacity = source.maxParticleCount
                            assertEquals(5f, source.lifetime.value, .0001f)
                            assertFalse(source.isMotionEnabled)
                            val originalSpin = listOf(source.rotationVelocityCurveX, source.rotationVelocityCurveY,
                                source.rotationVelocityCurveZ).map {
                                assertEquals(ParticleCurveType.RANDOM, it.curveType)
                                it.start to it.end
                            }
                            val clone = checkNotNull(original.clone(Entity.CloneOptions(
                                recursive = true, shouldShareMaterialInstance = false)))
                            try {
                                assertTrue(scope.addChild(clone))
                                val target = checkNotNull(clone.components[ParticleComponent::class.java]).mainSimulation
                                target.configureFinaleRain()
                                // These getters pull from native outside SceneEvents.Update batching.
                                assertEquals(size.value * 2f, target.startSize.value, .0001f)
                                assertEquals(size.range * 2f, target.startSize.range, .0001f)
                                assertEquals(speed.value * 1.5f, target.startSpeed.value, .0001f)
                                assertEquals(speed.range * 1.5f, target.startSpeed.range, .0001f)
                                assertEquals(capacity * 6, target.maxParticleCount)
                                assertEquals(4f, target.lifetime.value, .0001f)
                                assertTrue(target.isColorGradientEnabled)
                                listOf(target.rotationVelocityCurveX, target.rotationVelocityCurveY,
                                    target.rotationVelocityCurveZ).forEachIndexed { axis, curve ->
                                    assertEquals(originalSpin[axis].first * 1.5f, curve.start, .0001f)
                                    assertEquals(originalSpin[axis].second * 1.5f, curve.end, .0001f)
                                }
                                assertEquals(size, source.startSize)
                                assertEquals(speed, source.startSpeed)
                                assertEquals(capacity, source.maxParticleCount)
                                assertEquals(5f, source.lifetime.value, .0001f)
                                Log.i("FinaleRainNativeTest", "${original.getName()} size=${target.startSize} " +
                                    "speed=${target.startSpeed} life=${target.lifetime} capacity=${target.maxParticleCount}")
                            } finally { clone.destroy() }
                        }
                    } finally { root.destroy() }
                } finally {
                    bundle.releaseModel("Scenes/PetalRain")
                    bundle.close()
                }
            }
        }
    }
}
