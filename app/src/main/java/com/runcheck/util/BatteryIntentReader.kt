package com.runcheck.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter

object BatteryIntentReader {
    @Suppress("kotlin:S5322")
    fun readBatteryChangedStickyIntent(context: Context): Intent? =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
}
