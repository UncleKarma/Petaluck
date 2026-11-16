package com.example.flowertears.p18ab52cf.platform

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.util.Log
import com.example.flowertears.p18ab52cf.domain.DebugRoseColor
import com.example.flowertears.p18ab52cf.domain.RoseLayout

/** Only a running debuggable app registers this receiver; DUMP limits senders to shell/system. */
internal class RoseColorDebugReceiver(private val selection: DebugRoseColor) : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != action(context)) return
        val color = intent.extras?.get("color") as? Int
        val accepted = color != null && selection.set(color)
        resultCode = if (accepted) 0 else 1
        resultData = if (accepted)
            "selectedColor=${selection.selectedColor}; applies to future new rounds until changed or restarted"
        else "Invalid color; use integer -1..${RoseLayout.COLOR_COUNT - 1}; selectedColor=${selection.selectedColor} unchanged"
        Log.i("RoseDebug", resultData ?: "")
    }

    companion object {
        fun action(context: Context) = "${context.packageName}.DEBUG_ROSE_COLOR"
        fun register(context: Context, selection: DebugRoseColor) {
            if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
            context.registerReceiver(RoseColorDebugReceiver(selection), IntentFilter(action(context)),
                Manifest.permission.DUMP, null, Context.RECEIVER_EXPORTED)
        }
    }
}
