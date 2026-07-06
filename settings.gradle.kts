pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral {
            content {
                includeGroup("commons-codec")
                includeGroup("commons-io")
                includeGroup("commons-logging")
                includeGroup("commons-validator")
                includeGroup("joda-time")
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.(android|autonomousapps|esotericsoftware|fasterxml|github).*")
                includeGroupByRegex("com\\.(google|googlecode|h2database|h3xstream|hankcs|kichik).*")
                includeGroupByRegex("com\\.(moandjiezana|pinterest|samskivert|squareup|sun).*")
                includeGroupByRegex("de\\.siegmar.*")
                includeGroupByRegex("dev\\.detekt.*")
                includeGroupByRegex("dev\\.zacsweers.*")
                includeGroupByRegex("io\\.github\\..*")
                includeGroupByRegex("io\\.gitlab\\.arturbosch.*")
                includeGroupByRegex("io\\.(netty|opentelemetry).*")
                includeGroupByRegex("jakarta\\..*")
                includeGroupByRegex("javax\\..*")
                includeGroupByRegex("net\\.(gpedro|java|ltgt|sf).*")
                includeGroupByRegex("org\\.(anarres|apache|assertj|bitbucket|bouncycastle|checkerframework|ec4j|eclipse).*")
                includeGroupByRegex("org\\.(glassfish|gradle|hamcrest|jdom|jetbrains|jlleitschuh|json|jsoup|junit).*")
                includeGroupByRegex("org\\.(jspecify|jvnet|mockito|ow2|owasp|semver4j|slf4j|sonarqube|sonarsource).*")
                includeGroupByRegex("org\\.(sonatype|tensorflow|tukaani|yaml).*")
                includeGroupByRegex("us\\.springett.*")
            }
        }
        gradlePluginPortal {
            content {
                includeGroup("androidx.room")
                includeGroup("com.android.application")
                includeGroup("com.autonomousapps")
                includeGroup("com.autonomousapps.dependency-analysis")
                includeGroup("com.github.skydoves")
                includeGroup("com.github.skydoves.compose.stability.analyzer")
                includeGroup("com.google.dagger")
                includeGroup("com.google.dagger.hilt.android")
                includeGroup("com.google.devtools.ksp")
                includeGroup("dev.detekt")
                includeGroup("io.gitlab.arturbosch.detekt")
                includeGroup("org.gradle.toolchains")
                includeGroup("org.gradle.toolchains.foojay-resolver-convention")
                includeGroup("org.jetbrains.kotlin")
                includeGroup("org.jetbrains.kotlin.plugin.compose")
                includeGroup("org.jlleitschuh.gradle")
                includeGroup("org.jlleitschuh.gradle.ktlint")
                includeGroup("org.owasp")
                includeGroup("org.owasp.dependencycheck")
                includeGroup("org.sonarqube")
                includeGroup("org.sonarsource.scanner.gradle")
            }
        }
    }
}

// Version-catalog plugin aliases are not available in settings scripts.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven("https://jitpack.io")
            }
            filter {
                includeGroup("com.github.m-lab")
            }
        }
    }
}

rootProject.name = "Runcheck"
include(":app")
