buildscript {
    // Keep vulnerable Gradle plugin transitives on patched build-time versions.
    val securityPinnedBuildscriptModules =
        arrayOf(
            "com.fasterxml.jackson.core:jackson-annotations:2.22",
            "com.fasterxml.jackson.core:jackson-core:2.22.1",
            "com.fasterxml.jackson.core:jackson-databind:2.22.1",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.22.1",
            "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.22.0",
            "com.fasterxml.jackson.module:jackson-module-blackbird:2.22.0",
            "org.bitbucket.b_c:jose4j:0.9.6",
            "org.bouncycastle:bcpkix-jdk18on:1.84",
            "org.bouncycastle:bcprov-jdk18on:1.84",
            "org.bouncycastle:bcutil-jdk18on:1.84",
            "org.jdom:jdom2:2.0.6.1",
        )

    configurations.classpath {
        resolutionStrategy.force(*securityPinnedBuildscriptModules)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.owasp.dependency.check) apply false
    alias(libs.plugins.stability.analyzer) apply false
    alias(libs.plugins.sonarqube)
}

ktlint {
    version.set(libs.versions.ktlint.get())
    ignoreFailures.set(false)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

val sonarProjectProperties =
    java.util.Properties().apply {
        val file = rootProject.file("sonar-project.properties")
        if (file.isFile) {
            file.inputStream().use(::load)
        }
    }

val gradleManagedSonarProperties =
    setOf(
        "sonar.sources",
        "sonar.tests",
        "sonar.java.binaries",
        "sonar.java.test.binaries",
        "sonar.java.libraries",
        "sonar.java.test.libraries",
        "sonar.kotlin.binaries",
    )

sonar {
    properties {
        property("sonar.host.url", sonarProjectProperties.getProperty("sonar.host.url", "https://sonarcloud.io"))
        sonarProjectProperties.forEach { key, value ->
            val propertyName = key.toString()
            if (propertyName !in gradleManagedSonarProperties) {
                property(propertyName, value.toString())
            }
        }
    }
}

project(":app") {
    sonar {
        properties {
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                layout.buildDirectory
                    .file("reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml")
                    .get()
                    .asFile.absolutePath,
            )
        }
    }
}

val prepareSonarAndroidLintReport by tasks.registering {
    group = "verification"
    description = "Writes an empty Android Lint XML import for SonarCloud; tools/lc.ps1 owns real Android Lint findings."

    val reportFile = layout.projectDirectory.file("app/build/reports/lint-results-debug.xml")

    outputs.file(reportFile)
    outputs.upToDateWhen { false }
    mustRunAfter(":app:lintDebug")

    doLast {
        val file = reportFile.asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <issues format="6" by="lint">
            </issues>
            """.trimIndent() + "\n",
            Charsets.UTF_8,
        )
    }
}

tasks.named("sonar") {
    dependsOn(":app:assembleDebug", ":app:jacocoDebugUnitTestReport", prepareSonarAndroidLintReport)
}
