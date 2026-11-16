package com.unclekarma.petaluck

import com.unclekarma.petaluck.scene.AttachmentBinding
import com.unclekarma.petaluck.scene.SceneFrameGate
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class AttachmentBindingTest {
    private class Panel(var valid:Boolean=true)

    @Test fun missingInitialAttachmentCanBindLaterAndDoesNotBindTwice() {
        val binding=AttachmentBinding<Panel> {it.valid}
        var configurations=0
        assertNull(binding.bind(null) {configurations++})
        val panel=Panel()
        repeat(5) {assertSame(panel,binding.bind(panel) {configurations++})}
        assertEquals(1,configurations)
    }

    @Test fun invalidAttachmentCanBeReplacedByItsHost() {
        val binding=AttachmentBinding<Panel> {it.valid}
        var configurations=0
        val old=Panel()
        binding.bind(old) {configurations++};old.valid=false
        assertNull(binding.bind(old) {configurations++})
        val replacement=Panel()
        assertSame(replacement,binding.bind(replacement) {configurations++})
        assertEquals(2,configurations)
    }

    @Test fun failedConfigurationCanRetryTheSamePanel() {
        val binding=AttachmentBinding<Panel> {it.valid}
        val panel=Panel()
        try {binding.bind(panel) {error("not attached yet")};fail("Expected failure")}
        catch(e:IllegalStateException) {assertEquals("not attached yet",e.message)}
        assertSame(panel,binding.bind(panel) {})
    }

    @Test fun coldStartupWaitsUntilBothDelayedTextOwnersHaveBound()=runBlocking {
        val gate=SceneFrameGate()
        val resultBinding=AttachmentBinding<Panel> {it.valid}
        val messageBinding=AttachmentBinding<Panel> {it.valid}
        var result:Panel?=null
        var message:Panel?=null
        var loads=0
        val startup=async(start=CoroutineStart.UNDISPATCHED) {
            gate.awaitReady {
                val a=resultBinding.bind(result) {}
                val b=messageBinding.bind(message) {}
                if(a!=null && b!=null)Unit else null
            }
            loads++
        }
        gate.onFrame();yield();assertEquals(0,loads)
        result=Panel();gate.onFrame();yield();assertEquals(0,loads)
        message=Panel();gate.onFrame();startup.await()
        assertEquals(1,loads)
    }
}
