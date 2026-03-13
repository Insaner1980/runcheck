package com.devicepulse.ui.common

import android.content.Context
import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.devicepulse.R
import com.devicepulse.domain.model.ConnectionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Throwable.messageOr(defaultMessage: String): String =
    message?.takeUnless(String::isBlank) ?: defaultMessage

fun formatStorageSize(context: Context, bytes: Long): String =
    Formatter.formatShortFileSize(context, bytes)

fun formatDecimal(value: Number, fractionDigits: Int): String =
    String.format(Locale.getDefault(), "%.${fractionDigits}f", value.toDouble())

fun formatLocalizedDateTime(timestamp: Long, skeleton: String): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return SimpleDateFormat(pattern, locale).format(Date(timestamp))
}

fun isUnknownValue(value: String?): Boolean =
    value.isNullOrBlank() || value.equals("unknown", ignoreCase = true)

@Composable
fun formatPercent(value: Int): String = stringResource(R.string.value_percent, value)

@Composable
fun formatPercent(value: Float, fractionDigits: Int = 1): String =
    "${formatDecimal(value, fractionDigits)}${stringResource(R.string.unit_percent)}"

@Composable
fun formatTemperature(value: Float, fractionDigits: Int = 1): String =
    "${formatDecimal(value, fractionDigits)}${stringResource(R.string.unit_celsius)}"

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
    ConnectionType.CELLULAR -> networkSubtype
        ?.takeUnless(::isUnknownValue)
        ?: stringResource(R.string.connection_cellular)
    ConnectionType.NONE -> stringResource(R.string.connection_none)
}
