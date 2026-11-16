package com.unclekarma.petaluck

import com.unclekarma.petaluck.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MulticolorRoseTest {
    @Test fun multicolorIsSeededAndUsesExistingLayerAlbedosWithoutAdjacentRepeats() {
        val root = File(checkNotNull(System.getProperty("user.dir"))).let { if (it.name == "app") it else File(it, "app") }
        repeat(128) { seed ->
            for (count in 17..30) {
                val c = RoseLayout.assemble(seed.toLong(), count, RoseLayout.MULTICOLOR)
                assertEquals(c, RoseLayout.assemble(seed.toLong(), count, RoseLayout.MULTICOLOR))
                assertTrue(c.petals.map { it.colorId }.distinct().size > 1)
                c.petals.zipWithNext().forEach { (a, b) -> assertNotEquals(a.colorId, b.colorId) }
                c.petals.groupBy { it.variant.substringBefore('_') }.values.forEach {
                    assertNotEquals(it.first().colorId, it.last().colorId)
                }
                RoseLayout.texturePaths(c).forEach { assertTrue(it, File(root, "src/main/assets/$it").isFile) }
                val mono = RoseLayout.assemble(seed.toLong(), count, 0)
                assertEquals(mono.petals, c.petals.map { it.copy(colorId = 0) })
            }
        }
    }
    @Test fun nearbyPetalsAcrossAllLayersNeverShareAColor() {
        repeat(256) { seed ->
            for (count in 17..30) {
                val c = RoseLayout.assemble(seed.toLong(), count, RoseLayout.MULTICOLOR)
                val layers = c.petals.groupBy { it.variant.substringBefore('_') }.values.toList()
                for (a in layers.indices) for (b in a + 1 until layers.size) {
                    val cutoff = Math.toRadians((180.0 / layers[a].size + 180.0 / layers[b].size))
                    for (p in layers[a]) for (q in layers[b]) {
                        // Independent geometric check: crown-plane unit vectors, including 0/360 wrap.
                        val dot = (p.position.x * q.position.x + p.position.y * q.position.y) /
                            kotlin.math.sqrt((p.position.x*p.position.x + p.position.y*p.position.y) *
                                (q.position.x*q.position.x + q.position.y*q.position.y))
                        if (dot >= kotlin.math.cos(cutoff) - 0.00001) {
                            assertNotEquals("seed=$seed n=$count petals=${p.id},${q.id}", p.colorId, q.colorId)
                        }
                    }
                }
            }
        }
    }
    @Test fun nineRoundColorsHaveEqualDrawOpportunityAndRainbowCanBeExcluded() {
        val frequencies = IntArray(RoseLayout.COLOR_COUNT)
        repeat(9000) { frequencies[RoseLayout.sample(it.toLong()).colorId]++ }
        frequencies.forEach { assertTrue("frequency=$it", it in 800..1200) }
        repeat(256) {
            assertNotEquals(RoseLayout.MULTICOLOR, RoseLayout.sample(it.toLong(),
                excludedColors = setOf(RoseLayout.MULTICOLOR)).colorId)
        }
    }
    @Test fun rainUsesEightRealColorsAndSingleColorRoundsKeepThreeLayers() {
        repeat(64) { seed ->
            val colorful = RoseLayout.assemble(seed.toLong(), 24, RoseLayout.MULTICOLOR)
            val paths = RoseLayout.rainTexturePaths(colorful)
            assertEquals(8, paths.size)
            assertEquals((0..7).toSet(), paths.map { it.substringAfter("petal-").substringBefore('-').toInt() }.toSet())
            for (color in 0..7) {
                val mono = RoseLayout.assemble(seed.toLong(), 24, color)
                assertTrue(mono.petals.all { it.colorId == color })
                assertEquals(RoseLayout.texturePaths(color), RoseLayout.texturePaths(mono))
                assertEquals(RoseLayout.texturePaths(color), RoseLayout.rainTexturePaths(mono).toSet())
            }
        }
    }
}
