package com.devicepulse.ui.common

import android.content.Context
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.devicepulse.R
import com.devicepulse.domain.model.ConnectionType
import java.util.Locale

fun Throwable.messageOr(defaultMessage: String): String =
    message?.takeUnless(String::isBlank) ?: defaultMessage

fun formatStorageSize(context: Context, bytes: Long): String =
    Formatter.formatShortFileSize(context, bytes)

fun formatDecimal(value: Number, fractionDigits: Int): String =
    String.format(Locale.getDefault(), "%.${fractionDigits}f", value.toDouble())

fun formatPercent(value: Int): String = "$value%"

fun formatPercent(value: Float, fractionDigits: Int = 1): String =
    "${formatDecimal(value, fractionDigits)}%"

fun formatTemperature(value: Float, fractionDigits: Int = 1): String =
    "${formatDecimal(value, fractionDigits)}°C"

@Composable
fun scoreLabel(score: Int): String = when {
    score >= 90 -> stringResource(R.string.score_excellent)
    score >= 70 -> stringResource(R.string.score_good)
    score >= 50 -> stringResource(R.string.score_fair)
    else -> stringResource(R.string.score_poor)
}

@Composable
fun connectionDisplayLabel(
    connectionType: ConnectionType,
    wifiSsid: String?,
    networkSubtype: String?
): String = when (connectionType) {
    ConnectionType.WIFI -> wifiSsid ?: stringResource(R.string.connection_wifi)
    ConnectionType.CELLULAR -> networkSubtype ?: stringResource(R.string.connection_cellular)
    ConnectionType.NONE -> stringResource(R.string.connection_none)
}
