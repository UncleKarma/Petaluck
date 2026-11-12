package com.example.flowertears.p18ab52cf

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.flowertears.p18ab52cf.platform.LaunchActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real petal action and native replay layout on a PICO OS 6 target. */
@RunWith(AndroidJUnit4::class)
class ReplaySmokeTest {
    @Test fun nativeReplayRemainsVisibleAndClickableForTwentyRounds() {
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val originalFlags=automation.serviceInfo.flags
        try {
            automation.serviceInfo=automation.serviceInfo.apply {
                flags=flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
            ActivityScenario.launch(LaunchActivity::class.java).use {
                repeat(20) {round->
                    val deadline=SystemClock.uptimeMillis()+45_000
                    var replay: AccessibilityNodeInfo?=null
                    while (SystemClock.uptimeMillis()<deadline) {
                        replay=findNode(automation) {it.text?.toString()=="再试一次"}
                        if (replay!=null) break
                        val petal=findNode(automation) {
                            it.contentDescription?.toString()?.contains("月季花瓣")==true ||
                                it.text?.toString()=="月季花瓣"
                        }
                        if (petal!=null) click(petal)
                        SystemClock.sleep(120)
                    }
                    assertTrue("Round ${round+1} has no visible native replay button",replay!=null)
                    assertTrue("Round ${round+1} replay is not clickable",click(checkNotNull(replay)))
                    val startDeadline=SystemClock.uptimeMillis()+10_000
                    var nextStarted=false
                    while (SystemClock.uptimeMillis()<startDeadline) {
                        if (findNode(automation) {it.text?.toString()=="再试一次"}==null) {
                            nextStarted=true
                            break
                        }
                        SystemClock.sleep(100)
                    }
                    assertTrue("Round ${round+1} replay did not start loading",nextStarted)
                }
            }
        } finally {
            automation.serviceInfo=automation.serviceInfo.apply {flags=originalFlags}
        }
    }

    private fun findNode(automation: UiAutomation, match: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val displays=automation.windowsOnAllDisplays
        val pending=ArrayDeque<AccessibilityNodeInfo>()
        for (i in 0 until displays.size()) {
            displays.valueAt(i).forEach {window->window.root?.let(pending::add)}
        }
        while (pending.isNotEmpty()) {
            val node=pending.removeFirst()
            if (node.isVisibleToUser && node.packageName?.toString()=="com.example.flowertears.p18ab52cf" && match(node)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let(pending::add)
        }
        return null
    }

    private fun click(node: AccessibilityNodeInfo): Boolean {
        var target: AccessibilityNodeInfo?=node
        while (target!=null) {
            if (target.isEnabled && target.isClickable) return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            target=target.parent
        }
        return false
    }
}
