package com.devicepulse.data.billing

import android.content.Context

object ProStatusCache {
    private const val PREFS_NAME = "pro_status"
    private const val KEY_IS_PRO = "is_pro"

    fun isPro(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_PRO, false)

    fun setPro(context: Context, isPro: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_PRO, isPro)
            .apply()
    }
}
