package com.runcheck

import android.content.Context
import io.sentry.android.core.SentryAndroid

object SentryInit {
    fun init(context: Context) {
        SentryAndroid.init(context) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.isDebug = true
            options.tracesSampleRate = 1.0
            options.environment = "debug"
        }
    }
}
