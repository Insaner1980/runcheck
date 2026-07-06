package com.runcheck.ui.pro

import com.runcheck.R
import com.runcheck.pro.ProFeature
import org.junit.Assert.assertEquals
import org.junit.Test

class ProFeatureUiStringsTest {
    @Test
    fun `each pro feature maps to its user visible label resource`() {
        val expectedLabels =
            mapOf(
                ProFeature.EXTENDED_HISTORY to R.string.pro_feature_extended_history,
                ProFeature.CHARGER_COMPARISON to R.string.pro_feature_charger_comparison,
                ProFeature.PER_APP_BATTERY to R.string.pro_feature_per_app_battery,
                ProFeature.WIDGETS to R.string.pro_feature_widgets,
                ProFeature.CSV_EXPORT to R.string.pro_feature_csv_export,
                ProFeature.THERMAL_LOGS to R.string.pro_feature_thermal_logs,
                ProFeature.REMAINING_CHARGE_TIME to R.string.pro_feature_remaining_charge_time,
                ProFeature.STORAGE_CLEANUP to R.string.pro_feature_storage_cleanup,
            )

        assertEquals(ProFeature.entries.toSet(), expectedLabels.keys)
        expectedLabels.forEach { (feature, labelResId) ->
            assertEquals(labelResId, feature.labelResId())
        }
    }
}
