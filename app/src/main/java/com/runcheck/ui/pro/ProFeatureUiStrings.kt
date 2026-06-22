package com.runcheck.ui.pro

import androidx.annotation.StringRes
import com.runcheck.R
import com.runcheck.pro.ProFeature

@StringRes
internal fun ProFeature.labelResId(): Int =
    when (this) {
        ProFeature.EXTENDED_HISTORY -> R.string.pro_feature_extended_history
        ProFeature.CHARGER_COMPARISON -> R.string.pro_feature_charger_comparison
        ProFeature.PER_APP_BATTERY -> R.string.pro_feature_per_app_battery
        ProFeature.WIDGETS -> R.string.pro_feature_widgets
        ProFeature.CSV_EXPORT -> R.string.pro_feature_csv_export
        ProFeature.THERMAL_LOGS -> R.string.pro_feature_thermal_logs
        ProFeature.REMAINING_CHARGE_TIME -> R.string.pro_feature_remaining_charge_time
        ProFeature.STORAGE_CLEANUP -> R.string.pro_feature_storage_cleanup
    }
