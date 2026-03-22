package com.runcheck.ui.learn

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnArticleBodyFormatterTest {

    @Test
    fun parse_formatsHeadingsParagraphsAndInlineMarkup() {
        val blocks = LearnArticleBodyFormatter.parse(
            """
            ## Title

            Paragraph with **bold**, _italic_, and `code`.
            """.trimIndent()
        )

        assertEquals(2, blocks.size)
        assertEquals("Title", (blocks[0] as LearnArticleBlock.Heading).text.text)

        val paragraph = blocks[1] as LearnArticleBlock.Paragraph
        assertEquals("Paragraph with bold, italic, and code.", paragraph.text.text)
        assertTrue(paragraph.text.spanStyles.any { it.item.fontWeight == FontWeight.SemiBold })
        assertTrue(paragraph.text.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(paragraph.text.spanStyles.any { it.item.fontFamily == FontFamily.Monospace })
    }

    @Test
    fun parse_handlesBulletAndNumberedListsWithContinuationLines() {
        val blocks = LearnArticleBodyFormatter.parse(
            """
            ## Tips

            - First item
              continued line
            - Second item

            1. Step one
            2. Step two
            """.trimIndent()
        )

        assertEquals(3, blocks.size)

        val bulletList = blocks[1] as LearnArticleBlock.BulletList
        assertEquals(listOf("First item\ncontinued line", "Second item"), bulletList.items.map { it.text })

        val numberedList = blocks[2] as LearnArticleBlock.NumberedList
        assertEquals(listOf("Step one", "Step two"), numberedList.items.map { it.text })
    }

    @Test
    fun catalog_exposesSectionsAndStableLookup() {
        val article = LearnArticleCatalog.findById("battery_health")

        assertNotNull(article)
        assertEquals(
            LearnArticleCatalog.articles.size,
            LearnArticleCatalog.sections.sumOf { it.articles.size }
        )
        assertEquals(
            LearnArticleCatalog.sections.first { it.topic == LearnTopic.BATTERY }.articles,
            LearnArticleCatalog.articlesForTopic(LearnTopic.BATTERY)
        )
    }
}
