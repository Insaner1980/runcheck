package com.runcheck.ui.common

import com.runcheck.domain.insights.model.Insight

internal fun List<Insight>.changedUnseenIds(previousIds: Set<Long>): Set<Long>? {
    val unseenIds = filterNot(Insight::seen).map(Insight::id).toSet()
    return unseenIds.takeUnless { it == previousIds }
}
