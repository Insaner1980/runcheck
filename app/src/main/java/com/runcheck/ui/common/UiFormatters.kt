package com.runcheck.ui.common

import android.content.Context
import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.TemperatureUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Throwable.messageOr(defaultMessage: String): String =
    message?.takeUnless(String::isBlank) ?: defaultMessage

fun Throwable.messageOrRes(@androidx.annotation.StringRes defaultRes: Int): UiText =
    message?.takeUnless(String::isBlank)?.let { UiText.Dynamic(it) }
        ?: UiText.Resource(defaultRes)

fun formatStorageSize(context: Context, bytes: Long): String =
    Formatter.formatShortFileSize(context, bytes)

fun formatDecimal(value: Number, fractionDigits: Int): String =
    String.format(Locale.getDefault(), "%.${fractionDigits}f", value.toDouble())

fun formatLocalizedDateTime(timestamp: Long, skeleton: String): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return SimpleDateFormat(pattern, locale).format(Date(timestamp))
}

fun convertTemperature(valueCelsius: Number, unit: TemperatureUnit): Double {
    val value = valueCelsius.toDouble()
    return when (unit) {
        TemperatureUnit.CELSIUS -> value
        TemperatureUnit.FAHRENHEIT -> (value * 9.0 / 5.0) + 32.0
    }
}

@androidx.annotation.StringRes
fun temperatureUnitRes(unit: TemperatureUnit): Int = when (unit) {
    TemperatureUnit.CELSIUS -> R.string.unit_celsius
    TemperatureUnit.FAHRENHEIT -> R.string.unit_fahrenheit
}

fun formatTemperatureValue(
    valueCelsius: Number,
    unit: TemperatureUnit,
    fractionDigits: Int = 1
): String = formatDecimal(convertTemperature(valueCelsius, unit), fractionDigits)

fun formatTemperature(
    context: Context,
    valueCelsius: Number,
    unit: TemperatureUnit,
    fractionDigits: Int = 1
): String = context.getString(
    R.string.value_with_suffix_text,
    formatTemperatureValue(valueCelsius, unit, fractionDigits),
    context.getString(temperatureUnitRes(unit))
)

@Composable
fun rememberFormattedDateTime(timestamp: Long, skeleton: String): String =
    remember(timestamp, skeleton) {
        formatLocalizedDateTime(timestamp, skeleton)
    }

fun isUnknownValue(value: String?): Boolean =
    value.isNullOrBlank() || value.equals("unknown", ignoreCase = true)

@Composable
fun formatPercent(value: Int): String = stringResource(R.string.value_percent, value)

@Composable
fun formatPercent(value: Float, fractionDigits: Int = 1): String =
    stringResource(
        R.string.value_with_suffix_text,
        formatDecimal(value, fractionDigits),
        stringResource(R.string.unit_percent)
    )

@Composable
fun formatTemperature(
    valueCelsius: Float,
    unit: TemperatureUnit,
    fractionDigits: Int = 1
): String = stringResource(
    R.string.value_with_suffix_text,
    formatTemperatureValue(valueCelsius, unit, fractionDigits),
    stringResource(temperatureUnitRes(unit))
)

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
    ConnectionType.VPN -> stringResource(R.string.connection_vpn)
    ConnectionType.NONE -> stringResource(R.string.connection_none)
}

@Composable
fun batteryHealthLabel(health: BatteryHealth): String = when (health) {
    BatteryHealth.GOOD -> stringResource(R.string.battery_health_good)
    BatteryHealth.OVERHEAT -> stringResource(R.string.battery_health_overheat)
    BatteryHealth.DEAD -> stringResource(R.string.battery_health_dead)
    BatteryHealth.OVER_VOLTAGE -> stringResource(R.string.battery_health_over_voltage)
    BatteryHealth.COLD -> stringResource(R.string.battery_health_cold)
    BatteryHealth.UNKNOWN -> stringResource(R.string.battery_health_unknown)
}

@Composable
fun chargingStatusLabel(status: ChargingStatus): String = when (status) {
    ChargingStatus.CHARGING -> stringResource(R.string.charging_status_charging)
    ChargingStatus.DISCHARGING -> stringResource(R.string.charging_status_discharging)
    ChargingStatus.FULL -> stringResource(R.string.charging_status_full)
    ChargingStatus.NOT_CHARGING -> stringResource(R.string.charging_status_not_charging)
}

@Composable
fun plugTypeLabel(plugType: PlugType): String = when (plugType) {
    PlugType.AC -> stringResource(R.string.plug_type_ac)
    PlugType.USB -> stringResource(R.string.plug_type_usb)
    PlugType.WIRELESS -> stringResource(R.string.plug_type_wireless)
    PlugType.NONE -> stringResource(R.string.plug_type_none)
}

@Composable
fun temperatureBandLabel(temperatureC: Float): String = when {
    temperatureC >= 45f -> stringResource(R.string.thermal_critical)
    temperatureC >= 40f -> stringResource(R.string.thermal_hot)
    temperatureC >= 35f -> stringResource(R.string.thermal_warm)
    temperatureC >= 25f -> stringResource(R.string.thermal_normal)
    else -> stringResource(R.string.thermal_cool)
}

@Composable
fun signalQualityLabel(quality: SignalQuality): String = when (quality) {
    SignalQuality.EXCELLENT -> stringResource(R.string.signal_excellent)
    SignalQuality.GOOD -> stringResource(R.string.signal_good)
    SignalQuality.FAIR -> stringResource(R.string.signal_fair)
    SignalQuality.POOR -> stringResource(R.string.signal_poor)
    SignalQuality.NO_SIGNAL -> stringResource(R.string.connection_none)
}
