package com.runcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailInfoBannerPolicyTest {
    @Test
    fun `most severe eligible banner wins before catalog order`() {
        val selected =
            selectDetailInfoBanner(
                candidates =
                    listOf(
                        DetailInfoBannerCandidate(id = "catalog-first", severity = 1, catalogOrder = 0),
                        DetailInfoBannerCandidate(id = "severe", severity = 3, catalogOrder = 5),
                    ),
                dismissedIds = emptySet(),
                showInfoBanners = true,
            )

        assertEquals("severe", selected?.id)
    }

    @Test
    fun `catalog order breaks equal severity ties`() {
        val selected =
            selectDetailInfoBanner(
                candidates =
                    listOf(
                        DetailInfoBannerCandidate(id = "later", severity = 2, catalogOrder = 4),
                        DetailInfoBannerCandidate(id = "earlier", severity = 2, catalogOrder = 1),
                    ),
                dismissedIds = emptySet(),
                showInfoBanners = true,
            )

        assertEquals("earlier", selected?.id)
    }

    @Test
    fun `dismissed and ineligible banners are skipped`() {
        val selected =
            selectDetailInfoBanner(
                candidates =
                    listOf(
                        DetailInfoBannerCandidate(id = "dismissed", severity = 3, catalogOrder = 0),
                        DetailInfoBannerCandidate(id = "ineligible", severity = 2, catalogOrder = 1, eligible = false),
                        DetailInfoBannerCandidate(id = "visible", severity = 1, catalogOrder = 2),
                    ),
                dismissedIds = setOf("dismissed"),
                showInfoBanners = true,
            )

        assertEquals("visible", selected?.id)
    }

    @Test
    fun `global preference hides every banner`() {
        val selected =
            selectDetailInfoBanner(
                candidates = listOf(DetailInfoBannerCandidate("visible", severity = 1, catalogOrder = 0)),
                dismissedIds = emptySet(),
                showInfoBanners = false,
            )

        assertNull(selected)
    }
}
