package com.runcheck.testutil

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun findAppDir(): Path {
    val start = Paths.get("").toAbsolutePath()
    return generateSequence(start) { it.parent }
        .flatMap { path -> sequenceOf(path, path.resolve("app")) }
        .first { Files.exists(it.resolve("src/main/res")) && Files.exists(it.resolve("build.gradle.kts")) }
}

fun findRootDir(): Path {
    val start = Paths.get("").toAbsolutePath()
    return generateSequence(start) { it.parent }
        .first { Files.exists(it.resolve("gradle/libs.versions.toml")) }
}
