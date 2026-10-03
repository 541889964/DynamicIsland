package com.island.dynamic.receiver
import android.content.*
import android.os.BatteryManager
import com.island.dynamic.model.*
class BatteryMonitor(private val ctx: Context) : BroadcastReceiver() {
    fun register() { ctx.registerReceiver(this, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }
    fun unregister() { runCatching { ctx.unregisterReceiver(this) } }
    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        IslandStateHolder.batteryLevel = level
        val was = IslandStateHolder.isCharging
        IslandStateHolder.isCharging = charging
        if (charging && !was && IslandStateHolder.appMode == AppMode.LIFE) IslandStateHolder.mode = IslandMode.CHARGING
        if (!charging && was && IslandStateHolder.mode == IslandMode.CHARGING) IslandStateHolder.mode = IslandMode.IDLE
    }
}
