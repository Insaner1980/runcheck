package com.runcheck.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationArgumentContractTest {
    @Test
    fun fullscreenChartArgumentsKeepStableRouteContract() {
        assertEquals("source", Screen.FullscreenChart.ARG_SOURCE)
        assertEquals("metric", Screen.FullscreenChart.ARG_METRIC)
        assertEquals("period", Screen.FullscreenChart.ARG_PERIOD)
        assertEquals("fullscreen_chart/{source}/{metric}/{period}", Screen.FullscreenChart.ROUTE)
        assertEquals(
            "fullscreen_chart/BATTERY_HISTORY/LEVEL/DAY",
            Screen.FullscreenChart("BATTERY_HISTORY", "LEVEL", "DAY").route,
        )
    }

    @Test
    fun cleanupArgumentKeepsStableRouteContract() {
        assertEquals("type", Screen.Cleanup.ARG_TYPE)
        assertEquals("cleanup/{type}", Screen.Cleanup.ROUTE)
        assertEquals("cleanup/LARGE_FILES", Screen.Cleanup("LARGE_FILES").route)
    }

    @Test
    fun learnArticleArgumentKeepsStableRouteContract() {
        assertEquals("articleId", Screen.LearnArticle.ARG_ARTICLE_ID)
        assertEquals("learn/{articleId}", Screen.LearnArticle.ROUTE)
        assertEquals("learn/battery-health", Screen.LearnArticle("battery-health").route)
    }
}
