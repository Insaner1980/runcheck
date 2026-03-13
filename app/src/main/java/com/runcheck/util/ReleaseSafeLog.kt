package com.devicepulse.util

import android.util.Log
import com.devicepulse.BuildConfig

object ReleaseSafeLog {
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (!BuildConfig.DEBUG) return

        if (throwable != null) {
            Log.e(tag, message, throwable)
            return
        }
        Log.e(tag, message)
    }
}
