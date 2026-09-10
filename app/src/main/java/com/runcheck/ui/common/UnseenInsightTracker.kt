package com.runcheck.ui.common

import com.runcheck.domain.insights.model.Insight

internal class UnseenInsightTracker {
    private var previousIds: Set<Long> = emptySet()

    fun idsToMarkSeen(insights: List<Insight>): Set<Long>? {
        val unseenIds = insights.filterNot(Insight::seen).map(Insight::id).toSet()
        if (unseenIds == previousIds) return null

        previousIds = unseenIds
        return unseenIds.takeIf { it.isNotEmpty() }
    }
}
