package com.unclekarma.petaluck

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.unclekarma.petaluck.platform.LaunchActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Launch and visible native-Compose text smoke check; requires a PICO OS 6 target. */
@RunWith(AndroidJUnit4::class)
class LaunchSmokeTest {
    @Test fun launcherShowsTeachingWithoutAnActivityRestart() {
        ActivityScenario.launch(LaunchActivity::class.java).use {scenario->
            scenario.onActivity {activity->
                assertFalse(activity.isDestroyed)
                assertFalse(activity.isFinishing)
            }
            val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
            val originalFlags=automation.serviceInfo.flags
            val expected=setOf("心里想着一个人，也可以只为自己。","看向或靠近一片花瓣，捏住，轻轻拉开。")
            val seen=mutableSetOf<String>()
            var visibleTogether=false
            try {
                val info=automation.serviceInfo
                info.flags=originalFlags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                automation.serviceInfo=info
                val deadline=SystemClock.uptimeMillis()+20_000
                while(SystemClock.uptimeMillis()<deadline) {
                    val current=mutableSetOf<String>()
                    val displays=automation.windowsOnAllDisplays
                    for(i in 0 until displays.size()) {
                        for(window in displays.valueAt(i)) {
                            window.root?.let {collectVisibleText(it,current)}
                        }
                    }
                    seen+=current
                    if(current.containsAll(expected)){visibleTogether=true;break}
                    SystemClock.sleep(100)
                }
                assertTrue("Initial teaching was not visible without restarting; observed=$seen",visibleTogether)
            } finally {
                val info=automation.serviceInfo
                info.flags=originalFlags
                automation.serviceInfo=info
            }
        }
    }

    private fun collectVisibleText(root:AccessibilityNodeInfo,output:MutableSet<String>) {
        val pending=ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        var visited=0
        while(pending.isNotEmpty() && visited++<2000) {
            val node=pending.removeFirst()
            if(node.isVisibleToUser && node.packageName?.toString()=="com.unclekarma.petaluck") {
                node.text?.toString()?.let {output+=it}
            }
            for(i in 0 until node.childCount)node.getChild(i)?.let {pending.add(it)}
        }
    }
}
