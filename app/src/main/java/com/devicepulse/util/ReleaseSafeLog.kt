package com.devicepulse.util

import android.util.Log
import com.devicepulse.BuildConfig

object ReleaseSafeLog {
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG && throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
