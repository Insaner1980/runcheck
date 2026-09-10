package com.runcheck.data.battery

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.runcheck.data.device.DeviceProfile
import com.runcheck.util.AppDispatchers
import com.runcheck.util.BatteryIntentReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val MAX_PLAUSIBLE_CYCLE_COUNT = 10_000

internal fun normalizeCycleCount(value: Int): Int? = value.takeIf { it in 0..MAX_PLAUSIBLE_CYCLE_COUNT }

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
open class Android14BatterySource(
    context: Context,
    profile: DeviceProfile,
    private val dispatchers: AppDispatchers,
) : GenericBatterySource(context, profile, dispatchers) {
    override fun getCycleCount(): Flow<Int?> =
        flow {
            emit(readCycleCountFromBroadcast())
        }.flowOn(dispatchers.io)

    private fun readCycleCountFromBroadcast(): Int? =
        try {
            val intent = BatteryIntentReader.readBatteryChangedStickyIntent(context)
            val cycleCount = intent?.getIntExtra(EXTRA_CYCLE_COUNT, -1) ?: -1
            normalizeCycleCount(cycleCount)
        } catch (_: Exception) {
            null
        }

    companion object {
        // Broadcast extra added in API 34 — no special permission needed
        private const val EXTRA_CYCLE_COUNT = "android.os.extra.CYCLE_COUNT"
    }
}
