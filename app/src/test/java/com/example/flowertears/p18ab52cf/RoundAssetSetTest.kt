package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.scene.RoundAssetSet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RoundAssetSetTest {
    private class Texture(val path: String) {
        var valid = true
        var global = false
        var releases = 0
        fun destroyLastOwner() { if (!global) valid = false }
    }
    private fun cache() = RoundAssetSet<Texture>({ it.valid }, { it.global = true }, { it.releases++; it.valid = false })
    private fun paths(color: Int) = listOf("outer", "middle", "inner").map { "$color/$it" }.toSet()

    @Test fun twentyRoundsKeepOnlyThreeValidPinnedLayersAndReuseRepeatedColors() = runBlocking {
        val cache = cache(); val loaded = mutableListOf<Texture>()
        var previous = emptyList<Texture>()
        repeat(20) { round ->
            previous.forEach { it.destroyLastOwner() }
            val selected = paths((round / 2) % 8)
            cache.retain(selected) { Texture(it).also(loaded::add) }
            val current = selected.map { checkNotNull(cache[it]) }
            assertEquals(3, loaded.count { it.valid })
            assertTrue(current.all { it.global && it.valid })
            if (round % 2 == 1) current.zip(previous).forEach { (a,b) -> assertSame(a,b) }
            previous = current
        }
        assertEquals(30, loaded.size)
        cache.close(); cache.close()
        assertTrue(loaded.all { !it.valid && it.releases == 1 })
    }

    @Test fun failedNewColorClosesOldLayersAndRetriesWithoutDiscardingLoadedCompanion() = runBlocking {
        val cache = cache(); val old = mutableListOf<Texture>()
        cache.retain(paths(0)) { Texture(it).also(old::add) }
        var failLayer = true
        val load: suspend (String) -> Texture = {
            if (it == "4/middle" && failLayer) error("missing layer")
            Texture(it)
        }
        try { cache.retain(paths(4), load); fail("Expected layer failure") }
        catch (e: IllegalStateException) { assertEquals("missing layer", e.message) }
        assertTrue(old.all { !it.valid && it.releases == 1 })
        assertNull(cache["0/outer"])
        val outer = checkNotNull(cache["4/outer"])
        assertTrue(outer.global)
        failLayer = false; cache.retain(paths(4), load)
        assertSame(outer, cache["4/outer"])
        assertTrue(paths(4).all { cache[it]?.valid == true })
        cache.close()
    }

    @Test fun staleLayerReloadPreservesOtherTwoAndDisposalIsIdempotent() = runBlocking {
        val cache = cache()
        cache.retain(paths(4)) { Texture(it) }
        val outer = cache["4/outer"]; val inner = cache["4/inner"]
        val stale = checkNotNull(cache["4/middle"]); stale.valid = false
        cache.retain(paths(4)) { Texture(it) }
        assertSame(outer, cache["4/outer"]); assertSame(inner, cache["4/inner"])
        val replacement = checkNotNull(cache["4/middle"]); assertNotSame(stale, replacement)
        cache.close(); cache.close()
        assertEquals(1, replacement.releases); assertEquals(1, outer!!.releases); assertEquals(1, inner!!.releases)
        assertNull(cache["4/inner"])
    }
}
