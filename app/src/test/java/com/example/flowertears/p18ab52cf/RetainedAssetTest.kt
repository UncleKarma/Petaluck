package com.example.flowertears.p18ab52cf

import com.example.flowertears.p18ab52cf.scene.RetainedAsset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RetainedAssetTest {
    private class Texture {
        var valid=true
        var global=false
        var releases=0
        fun destroyLastRoundOwner() {if(!global)valid=false}
    }
    private fun cache()=RetainedAsset<Texture>({it.valid},{it.global=true},{it.releases++;it.valid=false})

    @Test fun sharedTextureSurvivesTwentyRoundTeardownsAndClosesOnce()=runBlocking {
        val cache=cache()
        var loads=0
        val first=cache.getOrLoad {loads++;Texture()}
        repeat(20) {
            first.destroyLastRoundOwner()
            assertSame(first,cache.getOrLoad {loads++;Texture()})
            assertTrue(first.valid)
        }
        assertEquals(1,loads)
        cache.close();cache.close()
        assertNull(cache.current);assertEquals(1,first.releases)
    }

    @Test fun invalidNativeHandleIsReloadedBeforeBinding()=runBlocking {
        val cache=cache()
        val old=cache.getOrLoad {Texture()}
        old.valid=false
        assertNull(cache.current)
        val replacement=cache.getOrLoad {Texture()}
        assertNotSame(old,replacement);assertTrue(replacement.global)
        cache.close();assertEquals(1,replacement.releases)
    }

    @Test fun failedAssetLoadCanBeRetried()=runBlocking {
        val cache=cache()
        try {cache.getOrLoad {error("load failed")};fail("Expected failure")}
        catch(e:IllegalStateException) {assertEquals("load failed",e.message)}
        assertNull(cache.current)
        assertTrue(cache.getOrLoad {Texture()}.valid)
        cache.close()
    }

    @Test fun failedPinReleasesPartialAssetAndDoesNotPoisonRetry()=runBlocking {
        var failPin=true
        val cache=RetainedAsset<Texture>({it.valid},{if(failPin)error("pin failed") else it.global=true},{it.releases++;it.valid=false})
        val partial=Texture()
        try {cache.getOrLoad {partial};fail("Expected failure")}
        catch(e:IllegalStateException) {assertEquals("pin failed",e.message)}
        assertEquals(1,partial.releases);assertNull(cache.current)
        failPin=false
        assertTrue(cache.getOrLoad {Texture()}.global)
        cache.close()
    }
}
