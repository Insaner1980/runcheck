package com.runcheck.service.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks screen on/off state and deep sleep vs held awake during screen-off,
 * accumulating battery usage stats per state since the last unplug event.
 */
@Singleton
class ScreenStateTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    // Screen state tracking
    private var screenOn = true
    private var lastTransitionTime = System.currentTimeMillis()
    private var lastTransitionLevel: Int? = null

    // Accumulated durations (ms)
    private var screenOnDurationMs: Long = 0L
    private var screenOffDurationMs: Long = 0L

    // Accumulated drain (battery percentage points)
    private var screenOnDrainPct: Float = 0f
    private var screenOffDrainPct: Float = 0f

    // Deep sleep vs held awake tracking (screen-off only)
    private var deepSleepDurationMs: Long = 0L
    private var heldAwakeDurationMs: Long = 0L
    private var lastIdleCheckTime: Long = 0L
    private var lastIdleState: Boolean = false

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> onScreenStateChanged(true)
                Intent.ACTION_SCREEN_OFF -> onScreenStateChanged(false)
                Intent.ACTION_POWER_DISCONNECTED -> resetAll()
            }
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)
        registered = true
        screenOn = powerManager.isInteractive
        lastTransitionTime = System.currentTimeMillis()
        lastTransitionLevel = getCurrentBatteryLevel()
        lastIdleCheckTime = lastTransitionTime
        lastIdleState = !screenOn && powerManager.isDeviceIdleMode
    }

    fun stop() {
        if (!registered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) { }
        registered = false
    }

    /**
     * Called periodically (e.g. every 2s from ViewModel flow) to update
     * idle mode tracking during screen-off periods.
     */
    fun tick() {
        if (screenOn) return
        val now = System.currentTimeMillis()
        val elapsed = now - lastIdleCheckTime
        if (elapsed <= 0) return

        val isIdle = powerManager.isDeviceIdleMode
        if (lastIdleState) {
            deepSleepDurationMs += elapsed
        } else {
            heldAwakeDurationMs += elapsed
        }
        lastIdleCheckTime = now
        lastIdleState = isIdle
    }

    fun getScreenUsageStats(): ScreenUsageStats? {
        val now = System.currentTimeMillis()
        val currentLevel = getCurrentBatteryLevel() ?: return null
        // Flush current period
        val elapsed = now - lastTransitionTime
        val totalOnMs = screenOnDurationMs + if (screenOn) elapsed else 0L
        val totalOffMs = screenOffDurationMs + if (!screenOn) elapsed else 0L

        // Current period drain
        val currentDrain = lastTransitionLevel?.let { startLevel ->
            (startLevel - currentLevel).coerceAtLeast(0).toFloat()
        } ?: 0f

        val onDrain = screenOnDrainPct + if (screenOn) currentDrain else 0f
        val offDrain = screenOffDrainPct + if (!screenOn) currentDrain else 0f

        val onRatePerHour = if (totalOnMs > 60_000) onDrain / (totalOnMs / 3_600_000f) else null
        val offRatePerHour = if (totalOffMs > 60_000) offDrain / (totalOffMs / 3_600_000f) else null

        if (totalOnMs == 0L && totalOffMs == 0L) return null

        return ScreenUsageStats(
            screenOnDurationMs = totalOnMs,
            screenOffDurationMs = totalOffMs,
            screenOnDrainPct = onDrain,
            screenOffDrainPct = offDrain,
            screenOnDrainRate = onRatePerHour,
            screenOffDrainRate = offRatePerHour
        )
    }

    fun getSleepAnalysis(): SleepAnalysis? {
        // Flush current screen-off period
        if (!screenOn) tick()

        val totalDeep = deepSleepDurationMs
        val totalAwake = heldAwakeDurationMs
        if (totalDeep == 0L && totalAwake == 0L) return null

        return SleepAnalysis(
            deepSleepDurationMs = totalDeep,
            heldAwakeDurationMs = totalAwake
        )
    }

    private fun onScreenStateChanged(isScreenOn: Boolean) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastTransitionTime
        val currentLevel = getCurrentBatteryLevel()
        val lastLevel = lastTransitionLevel
        val drain = if (lastLevel != null && currentLevel != null) {
            (lastLevel - currentLevel).coerceAtLeast(0).toFloat()
        } else 0f

        if (screenOn) {
            screenOnDurationMs += elapsed
            screenOnDrainPct += drain
        } else {
            screenOffDurationMs += elapsed
            screenOffDrainPct += drain
            // Flush idle tracking for the off period
            val idleElapsed = now - lastIdleCheckTime
            if (idleElapsed > 0) {
                if (lastIdleState) deepSleepDurationMs += idleElapsed
                else heldAwakeDurationMs += idleElapsed
            }
        }

        screenOn = isScreenOn
        lastTransitionTime = now
        lastTransitionLevel = currentLevel
        lastIdleCheckTime = now
        lastIdleState = !isScreenOn && powerManager.isDeviceIdleMode
    }

    private fun resetAll() {
        screenOnDurationMs = 0L
        screenOffDurationMs = 0L
        screenOnDrainPct = 0f
        screenOffDrainPct = 0f
        deepSleepDurationMs = 0L
        heldAwakeDurationMs = 0L
        lastTransitionTime = System.currentTimeMillis()
        lastTransitionLevel = getCurrentBatteryLevel()
        lastIdleCheckTime = lastTransitionTime
        lastIdleState = false
        screenOn = powerManager.isInteractive
    }

    private fun getCurrentBatteryLevel(): Int? {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return null
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level in 0..100) level else null
    }
}

data class ScreenUsageStats(
    val screenOnDurationMs: Long,
    val screenOffDurationMs: Long,
    val screenOnDrainPct: Float,
    val screenOffDrainPct: Float,
    val screenOnDrainRate: Float?,  // %/h, null if not enough data
    val screenOffDrainRate: Float?  // %/h
)

data class SleepAnalysis(
    val deepSleepDurationMs: Long,
    val heldAwakeDurationMs: Long
)
