package com.runcheck.ui.components.info

import androidx.annotation.StringRes

data class InfoSheetContent(
    @param:StringRes val title: Int,
    @param:StringRes val explanation: Int,
    @param:StringRes val normalRange: Int,
    @param:StringRes val whyItMatters: Int,
    @param:StringRes val deeperDetail: Int? = null
)
