package com.runcheck.ui.components.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfoCardCatalogTest {

    @Test
    fun `all card ids are unique and explicitly versioned`() {
        val cardIds = InfoCardCatalog.all.map { it.id }

        assertEquals(cardIds.size, cardIds.toSet().size)
        assertTrue(cardIds.all { it.matches(Regex(".+_v\\d+")) })
    }
}
