package com.runcheck.ui.learn

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

sealed interface LearnArticleBlock {
    data class Heading(
        val text: AnnotatedString,
    ) : LearnArticleBlock

    data class Paragraph(
        val text: AnnotatedString,
    ) : LearnArticleBlock

    data class BulletList(
        val items: List<AnnotatedString>,
    ) : LearnArticleBlock

    data class NumberedList(
        val items: List<AnnotatedString>,
    ) : LearnArticleBlock
}

object LearnArticleBodyFormatter {
    const val URL_ANNOTATION_TAG = "learn_url"
    private const val HEADING_PREFIX = "## "
    private val bulletPrefixRegex = Regex("""^[-*]\s+(.+)$""")
    private val numberedPrefixRegex = Regex("""^\d+[.)]\s+(.+)$""")
    private val inlineMarkers =
        listOf(
            InlineMarker("**", SpanStyle(fontWeight = FontWeight.SemiBold)),
            InlineMarker("__", SpanStyle(fontWeight = FontWeight.SemiBold)),
            InlineMarker("`", SpanStyle(fontFamily = FontFamily.Monospace)),
            InlineMarker("*", SpanStyle(fontStyle = FontStyle.Italic)),
            InlineMarker("_", SpanStyle(fontStyle = FontStyle.Italic)),
        )
    private val urlTokenRegex =
        Regex("""(?:https?://|www\.|[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+)\S*""")
    private val linkStyle =
        SpanStyle(
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
        )

    fun parse(body: String): List<LearnArticleBlock> {
        val lines =
            body
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim()
                .lines()

        if (lines.isEmpty() || lines.all(String::isBlank)) {
            return emptyList()
        }

        val blocks = mutableListOf<LearnArticleBlock>()
        var index = 0

        while (index < lines.size) {
            val trimmedLine = lines[index].trim()

            if (trimmedLine.isBlank()) {
                index += 1
                continue
            }

            when {
                trimmedLine.startsWith(HEADING_PREFIX) -> {
                    blocks +=
                        LearnArticleBlock.Heading(
                            parseInlineMarkup(trimmedLine.removePrefix(HEADING_PREFIX).trim()),
                        )
                    index += 1
                }

                trimmedLine.isBulletListItem() -> {
                    val (items, nextIndex) =
                        parseList(
                            lines = lines,
                            startIndex = index,
                            itemContent = { line -> bulletPrefixRegex.matchEntire(line.trim())?.groupValues?.get(1) },
                        )
                    blocks += LearnArticleBlock.BulletList(items = items.map(::parseInlineMarkup))
                    index = nextIndex
                }

                trimmedLine.isNumberedListItem() -> {
                    val (items, nextIndex) =
                        parseList(
                            lines = lines,
                            startIndex = index,
                            itemContent = { line -> numberedPrefixRegex.matchEntire(line.trim())?.groupValues?.get(1) },
                        )
                    blocks += LearnArticleBlock.NumberedList(items = items.map(::parseInlineMarkup))
                    index = nextIndex
                }

                else -> {
                    val paragraphLines = mutableListOf(trimmedLine)
                    index += 1

                    while (index < lines.size) {
                        val nextTrimmedLine = lines[index].trim()
                        if (nextTrimmedLine.isBlank()) {
                            index += 1
                            break
                        }
                        if (nextTrimmedLine.startsWith(HEADING_PREFIX) ||
                            nextTrimmedLine.isBulletListItem() ||
                            nextTrimmedLine.isNumberedListItem()
                        ) {
                            break
                        }

                        paragraphLines += nextTrimmedLine
                        index += 1
                    }

                    blocks +=
                        LearnArticleBlock.Paragraph(
                            parseInlineMarkup(paragraphLines.joinToString(separator = "\n")),
                        )
                }
            }
        }

        return blocks
    }

    private fun parseList(
        lines: List<String>,
        startIndex: Int,
        itemContent: (String) -> String?,
    ): Pair<List<String>, Int> {
        val items = mutableListOf<StringBuilder>()
        var index = startIndex

        while (index < lines.size) {
            val trimmedLine = lines[index].trim()

            if (trimmedLine.isBlank()) {
                index += 1
                break
            }

            if (trimmedLine.startsWith(HEADING_PREFIX)) {
                break
            }

            val content = itemContent(lines[index])
            if (content != null) {
                items += StringBuilder(content.trim())
                index += 1
                continue
            }

            val currentItem = items.lastOrNull() ?: break
            currentItem.append('\n').append(trimmedLine)
            index += 1
        }

        return items.map { it.toString().trim() } to index
    }

    private fun parseInlineMarkup(text: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        var index = 0

        while (index < text.length) {
            val marker =
                inlineMarkers.firstOrNull { candidate ->
                    text.startsWith(candidate.token, startIndex = index) &&
                        findClosingToken(text, index + candidate.token.length, candidate.token) != -1
                }

            if (marker == null) {
                val urlMatch = findUrlMatch(text = text, startIndex = index)
                if (urlMatch != null) {
                    val linkStart = builder.length
                    builder.pushStringAnnotation(tag = URL_ANNOTATION_TAG, annotation = urlMatch.url)
                    builder.append(urlMatch.displayText)
                    builder.pop()
                    builder.addStyle(linkStyle, linkStart, builder.length)
                    index += urlMatch.displayText.length
                    continue
                }
                builder.append(text[index])
                index += 1
                continue
            }

            val contentStart = index + marker.token.length
            val contentEnd = findClosingToken(text, contentStart, marker.token)
            if (contentEnd == -1) {
                builder.append(text[index])
                index += 1
                continue
            }

            val content = text.substring(contentStart, contentEnd)
            if (content.isBlank()) {
                builder.append(marker.token)
                index = contentStart
                continue
            }

            val spanStart = builder.length
            builder.append(content)
            builder.addStyle(marker.style, spanStart, builder.length)
            index = contentEnd + marker.token.length
        }

        return builder.toAnnotatedString()
    }

    private fun findClosingToken(
        text: String,
        fromIndex: Int,
        token: String,
    ): Int {
        var searchFrom = fromIndex

        while (searchFrom < text.length) {
            val foundIndex = text.indexOf(token, startIndex = searchFrom)
            if (foundIndex == -1) {
                return -1
            }
            if (foundIndex > fromIndex) {
                return foundIndex
            }
            searchFrom = foundIndex + 1
        }

        return -1
    }

    private fun findUrlMatch(
        text: String,
        startIndex: Int,
    ): UrlMatch? {
        val rawMatch =
            urlTokenRegex
                .find(text, startIndex)
                ?.takeIf { it.range.first == startIndex }
                ?.value
                ?: return null

        val displayText = rawMatch.trimEnd('.', ',', ';', ':', ')', ']', '}')
        if (displayText.isBlank() || '.' !in displayText) return null

        val url =
            if (
                displayText.startsWith("http://", ignoreCase = true) ||
                displayText.startsWith("https://", ignoreCase = true)
            ) {
                displayText
            } else {
                "https://$displayText"
            }

        return UrlMatch(
            displayText = displayText,
            url = url,
        )
    }

    private fun String.isBulletListItem(): Boolean = bulletPrefixRegex.matches(trim())

    private fun String.isNumberedListItem(): Boolean = numberedPrefixRegex.matches(trim())

    private data class InlineMarker(
        val token: String,
        val style: SpanStyle,
    )

    private data class UrlMatch(
        val displayText: String,
        val url: String,
    )
}
