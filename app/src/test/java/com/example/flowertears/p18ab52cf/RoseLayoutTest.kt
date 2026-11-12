package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.domain.*
import java.io.File
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class RoseLayoutTest {
    @Test fun whorlsSurroundTheBudAndInnerPetalsGrowUpward() {
        val head=RoseLayout.HEAD_PITCH*PI.toFloat()/180f
        assertTrue(-sin(head)>cos(head)) // Growth favors sky while still facing the viewer.
        assertTrue(cos(head)>0f)
        assertEquals(90f+RoseLayout.HEAD_PITCH,RoseLayout.CORE_PITCH,.0001f)
        for(n in 17..30) for(seed in 0L..19L) {
            val config=RoseLayout.assemble(seed,n,0)
            val whorls=listOf("outer","middle","inner").map {layer->config.petals.filter {it.variant.startsWith(layer)}}
            val averageTilt=whorls.map {ring->ring.map {it.tilt}.average()}
            assertTrue(averageTilt[0]<averageTilt[1]);assertTrue(averageTilt[1]<averageTilt[2])
            val radii=whorls.map {ring->ring.map {sqrt(it.position.x.pow(2)+it.position.y.pow(2))}.average()}
            assertTrue(radii[0]<radii[1]);assertTrue(radii[1]<radii[2])
            assertTrue(radii[2]<.012) // The roots follow the widening receptacle wall.
            assertTrue(config.petals.all {it.position.z<0f})
            assertTrue(config.petals.all {abs(it.roll)<=RoseLayout.ROLL_JITTER})
            for(ring in whorls) {
                val angles=ring.map {((it.angle%360)+360)%360}.sorted()
                val gaps=angles.zipWithNext {a,b->b-a}+listOf(angles.first()+360-angles.last())
                assertTrue(gaps.max()<360f/ring.size*1.25f)
            }
            for(p in whorls.last()) {
                val tilt=p.tilt*PI.toFloat()/180f
                val angle=p.angle*PI.toFloat()/180f
                // The inner petal's longitudinal direction stays above its base at every azimuth.
                val worldY=cos(angle)*cos(tilt)*cos(head)-sin(tilt)*sin(head)
                assertTrue("Inner tip must rise around the complete bud",worldY>0f)
            }
        }
    }

    @Test fun exportProductionLayoutsForMeshReview() {
        // The review tool reads these real Kotlin outputs rather than copying the layout algorithm.
        val configs=(17..30).map {RoseLayout.assemble(20261110L,it,1)}+
            (0..7).map {RoseLayout.assemble(20261110L,24,it)}+
            (17..30).flatMap {n->(0L..19L).map {seed->RoseLayout.assemble(seed,n,1)}}
        val rounds=configs.joinToString(",") {c->
            val petals=c.petals.joinToString(",") {p->
                """{"id":${p.id},"variant":"${p.variant}","textureFile":"${RoseLayout.texturePath(c.colorId,p.variant)}","uvIndex":${p.uvIndex},"albedoScale":${p.albedoScale},"position":[${p.position.x},${p.position.y},${p.position.z}],"angle":${p.angle},"tilt":${p.tilt},"roll":${p.roll},"scale":${p.scale}}"""
            }
            """{"seed":${c.seed},"count":${c.count},"colorId":${c.colorId},"petals":[$petals]}"""
        }
        File("build/reports/rose-layouts.json").apply {
            parentFile?.mkdirs()
            val binds=listOf(RoseStem.root,RoseStem.mid,RoseStem.tip).joinToString(",") { "[${it.x},${it.y},${it.z}]" }
            val variation="""{"phase":${RoseLayout.WHORL_PHASE_JITTER},"angleFraction":${RoseLayout.ANGLE_JITTER_FRACTION},"whorlTilt":${RoseLayout.WHORL_TILT_JITTER},"petalTilt":${RoseLayout.PETAL_TILT_JITTER},"whorlScale":${RoseLayout.WHORL_SCALE_JITTER},"petalScale":${RoseLayout.PETAL_SCALE_JITTER},"roll":${RoseLayout.ROLL_JITTER}}"""
            val frames=listOf(0f,.2f,.4f,.6f,.8f).joinToString(",") {time->
                val frame=RoseEntrance.sample(time)
                """{"time":$time,"scale":${frame.scale},"opacity":${frame.opacity},"finished":${frame.finished}}"""
            }
            val presentation="""{"rootOffset":[0,${RoseEntrance.ROOT_OFFSET_Y},0],"entranceSeconds":${RoseEntrance.DURATION_SECONDS},"frames":[$frames]}"""
            writeText("""{"layoutVersion":"${RoseLayout.VERSION}","headPitch":${RoseLayout.HEAD_PITCH},"corePitch":${RoseLayout.CORE_PITCH},"coreLocalPitch":${RoseLayout.CORE_LOCAL_PITCH},"stemBindPositions":[$binds],"variation":$variation,"presentation":$presentation,"rounds":[$rounds]}""")
        }
        assertEquals(302,configs.size)
    }

    @Test fun petalsHaveDistinctSeededFinishesWithBalancedLayerBrightness() {
        for (n in 17..30) for (seed in 0L..30L) {
            val config=RoseLayout.assemble(seed,n,3)
            assertEquals(config,RoseLayout.assemble(seed,n,3))
            for (layer in RoseLayout.layerNames) {
                val petals=config.petals.filter {it.variant.startsWith(layer)}
                assertEquals(petals.size,petals.map {it.albedoScale}.toSet().size)
                assertEquals(setOf(0,1),petals.map {it.uvIndex}.toSet())
                assertEquals(.93,petals.map {it.albedoScale}.average(),.000001)
                assertTrue(petals.all {it.albedoScale in .86f..1f})
            }
        }
        assertNotEquals(
            RoseLayout.assemble(11,30,3).petals.map {it.albedoScale to it.uvIndex},
            RoseLayout.assemble(12,30,3).petals.map {it.albedoScale to it.uvIndex}
        )
    }

    @Test fun arrangementsMixNeighboringLayersAndVaryAcrossSeededRounds() {
        val rounds=(0L..19L).map {RoseLayout.assemble(it,24,1)}
        for (layer in RoseLayout.layerNames) {
            val rings=rounds.map {c->c.petals.filter {it.variant.startsWith(layer)}}
            val angles=rings.first().map {it.angle}.sorted()
            val gaps=angles.zipWithNext {a,b->b-a}+listOf(angles.first()+360-angles.last())
            assertTrue(gaps.max()-gaps.min()>2f)
            val scales=rings.flatten().map {it.scale}
            assertTrue(scales.max()-scales.min()>.005f)
            val openings=rings.first().map {it.tilt}
            assertTrue(openings.max()-openings.min()>20f)
            assertEquals(20,rings.map {ring->ring.first().angle}.toSet().size)
        }
        val petals=rounds.first().petals
        val outer=petals.filter {it.variant.startsWith("outer")}
        val middle=petals.filter {it.variant.startsWith("middle")}
        val inner=petals.filter {it.variant.startsWith("inner")}
        assertTrue(outer.maxOf {it.tilt}>middle.minOf {it.tilt}+2f)
        assertTrue(middle.maxOf {it.tilt}>inner.minOf {it.tilt}+2f)
        assertTrue(outer.maxOf {it.position.z}>middle.minOf {it.position.z})
        assertTrue(middle.maxOf {it.position.z}>inner.minOf {it.position.z})
    }
}
