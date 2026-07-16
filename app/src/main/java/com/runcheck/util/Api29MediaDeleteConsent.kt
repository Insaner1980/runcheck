package com.runcheck.util

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import androidx.annotation.RequiresApi

@RequiresApi(29)
internal fun api29RecoverableDeleteAction(error: SecurityException): PendingIntent? =
    (error as? RecoverableSecurityException)?.userAction?.actionIntent
