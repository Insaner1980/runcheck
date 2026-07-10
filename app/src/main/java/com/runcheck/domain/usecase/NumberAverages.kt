package com.runcheck.domain.usecase

internal fun List<Int>.averageOrNull(): Int? = takeIf(List<Int>::isNotEmpty)?.average()?.toInt()
