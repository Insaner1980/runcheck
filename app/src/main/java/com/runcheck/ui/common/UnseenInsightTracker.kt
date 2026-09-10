package com.runcheck.ui.common

import com.runcheck.domain.insights.model.Insight

internal class UnseenInsightTracker {
    private var previousIds: Set<Long> = emptySet()
    private var pendingIds: Set<Long>? = null

    fun idsToMarkSeen(insights: List<Insight>): Set<Long>? {
        val unseenIds = insights.filterNot(Insight::seen).map(Insight::id).toSet()
        if (unseenIds.isEmpty()) {
            previousIds = emptySet()
            pendingIds = null
            return null
        }
        if (unseenIds == previousIds || unseenIds == pendingIds) return null

        pendingIds = unseenIds
        return unseenIds
    }

    fun complete(
        ids: Set<Long>,
        succeeded: Boolean,
    ) {
        if (pendingIds != ids) return
        if (succeeded) previousIds = ids
        pendingIds = null
    }
}
