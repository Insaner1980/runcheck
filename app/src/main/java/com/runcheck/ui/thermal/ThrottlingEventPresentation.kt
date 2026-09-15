package com.runcheck.ui.thermal

import androidx.compose.ui.graphics.Color
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.ThermalStatusPersistence
import com.runcheck.ui.theme.StatusColors

internal fun StatusColors.forThrottlingEvent(persistedStatus: String): Color =
    when (ThermalStatusPersistence.fromId(persistedStatus)) {
        ThermalStatus.SEVERE -> poor
        ThermalStatus.CRITICAL, ThermalStatus.EMERGENCY, ThermalStatus.SHUTDOWN -> critical
        ThermalStatus.NONE, ThermalStatus.LIGHT, ThermalStatus.MODERATE -> fair
        null -> unavailable
    }
