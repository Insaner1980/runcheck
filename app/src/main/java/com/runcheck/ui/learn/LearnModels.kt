package com.runcheck.ui.learn

import androidx.annotation.StringRes
import com.runcheck.R

enum class LearnTopic(@StringRes val labelRes: Int) {
    BATTERY(R.string.learn_topic_battery),
    TEMPERATURE(R.string.learn_topic_temperature),
    NETWORK(R.string.learn_topic_network),
    STORAGE(R.string.learn_topic_storage),
    GENERAL(R.string.learn_topic_general)
}

data class LearnArticle(
    val id: String,
    val topic: LearnTopic,
    @param:StringRes val titleRes: Int,
    @param:StringRes val previewRes: Int,
    @param:StringRes val bodyRes: Int,
    val readTimeMinutes: Int,
    val crossLinkRoute: String?
)
