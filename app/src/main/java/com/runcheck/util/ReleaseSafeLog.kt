package com.runcheck.util

import android.util.Log
import com.runcheck.BuildConfig

object ReleaseSafeLog {
    fun warn(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        runCatching {
            if (throwable != null) {
                Log.w(tag, message, throwable)
                return
            }
            Log.w(tag, message)
        }
    }

    fun error(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        runCatching {
            if (throwable != null) {
                Log.e(tag, message, throwable)
                return
            }
            Log.e(tag, message)
        }
    }
}
