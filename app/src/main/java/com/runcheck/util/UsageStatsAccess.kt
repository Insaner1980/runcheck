package com.runcheck.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Process

fun Context.hasUsageStatsAccess(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode =
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName,
        )
    return mode == AppOpsManager.MODE_ALLOWED
}
