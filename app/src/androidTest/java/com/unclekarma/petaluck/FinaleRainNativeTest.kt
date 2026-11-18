package com.unclekarma.petaluck

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ParticleComponent
import com.pico.spatial.core.ecs.particle.ParticleCurveType
import com.pico.spatial.core.ecs.resource.AssetBundle
import com.unclekarma.petaluck.platform.LaunchActivity
import com.unclekarma.petaluck.scene.configureFinaleEmitter
import com.unclekarma.petaluck.scene.EndingPetalRain
import com.unclekarma.petaluck.domain.RoseLayout
import com.pico.spatial.core.ecs.particle.ParticleEmitterShapeType
import com.pico.spatial.core.ecs.particle.ParticleEmissionLocation
import com.pico.spatial.core.ecs.particle.ParticleEmissionSpace
import androidx.test.platform.app.InstrumentationRegistry
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
                        val totalRate = originals.sumOf {
                            checkNotNull(it.components[ParticleComponent::class.java]).birthRateFull.value.toDouble()
                        }.toFloat() * 6f
                        assertEquals(72f, totalRate, .0001f)
                        for (original in originals) {
                            val sourceComponent = checkNotNull(original.components[ParticleComponent::class.java])
                            val source = sourceComponent.mainSimulation
                            val shape = sourceComponent.emitterShape
                            val birthRate = sourceComponent.birthRateFull
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
                                val component = checkNotNull(clone.components[ParticleComponent::class.java])
                                component.configureFinaleEmitter(totalRate)
                                val target = component.mainSimulation
                                // These getters pull from native outside SceneEvents.Update batching.
                                assertEquals(9f, component.birthRateFull.value, .0001f)
                                assertEquals(ParticleEmitterShapeType.CYLINDER, component.emitterShape.type)
                                assertEquals(ParticleEmissionLocation.VOLUME, component.emitterShape.emissionLocation)
                                assertEquals(ParticleEmissionSpace.LOCAL, component.emitterShape.emissionSpace)
                                assertEquals(-1f, component.emitterShape.emissionDirection.y, .0001f)
                                assertEquals(9.4f, component.emitterShape.size.x, .0001f)
                                assertEquals(.001f, component.emitterShape.size.y, .0001f)
                                assertEquals(9.4f, component.emitterShape.size.z, .0001f)
                                assertEquals(360f, component.emitterShape.radialSpan, .0001f)
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
                                assertEquals(shape, sourceComponent.emitterShape)
                                assertEquals(birthRate, sourceComponent.birthRateFull)
                                Log.i("FinaleRainNativeTest", "${original.getName()} size=${target.startSize} " +
                                    "speed=${target.startSpeed} life=${target.lifetime} capacity=${target.maxParticleCount} " +
                                    "rate=${component.birthRateFull} shape=${component.emitterShape}")
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

    @Test fun productionFinalePreparesEightColorResources() {
        ActivityScenario.launch(LaunchActivity::class.java).use {
            runBlocking(Dispatchers.Main) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                EndingPetalRain(context).use { rain ->
                    val config = RoseLayout.assemble(0x46494e414cL, 24, RoseLayout.MULTICOLOR)
                    assertEquals(8, RoseLayout.rainTexturePaths(config).distinct().size)
                    rain.prepare(1, config, surrounding = true)
                    // Production emits PetalRainReady only after all eight native owners/materials bind.
                }
            }
        }
    }
}
