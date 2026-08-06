package com.runcheck.util

import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Reads a project file for source-scanning contract assertions, normalising line
 * endings to `\n`.
 *
 * Windows checkouts materialise these files with CRLF, so multi-line `contains(...)`
 * patterns written with `\n` silently fail locally while passing on CI. Normalising at
 * the single read point keeps every contract assertion environment-independent.
 */
fun Path.readContractText(): String = readText().normalizeLineEndings()

fun File.readContractText(): String = readText().normalizeLineEndings()

private fun String.normalizeLineEndings(): String = replace("\r\n", "\n")
