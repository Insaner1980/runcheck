package com.runcheck.ui.learn

import androidx.annotation.StringRes

enum class LearnTopic { BATTERY, TEMPERATURE, NETWORK, STORAGE, GENERAL }

data class LearnArticle(
    val id: String,
    val topic: LearnTopic,
    @param:StringRes val titleRes: Int,
    @param:StringRes val previewRes: Int,
    @param:StringRes val bodyRes: Int,
    val readTimeMinutes: Int,
    val crossLinkRoute: String?
)
