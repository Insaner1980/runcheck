import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.StringReader
import java.util.Properties
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.owasp.dependency.check)
    alias(libs.plugins.stability.analyzer)
    jacoco
}

abstract class BuildFeatureAccess
    @Inject
    constructor(
        val buildFeatures: BuildFeatures,
    )

val configurationCacheRequested =
    objects
        .newInstance<BuildFeatureAccess>()
        .buildFeatures
        .configurationCache
        .requested
        .getOrElse(false)

val releaseSigningEnvNames =
    listOf(
        "RUNCHECK_KEYSTORE_PATH",
        "RUNCHECK_KEYSTORE_PASSWORD",
        "RUNCHECK_KEY_ALIAS",
        "RUNCHECK_KEY_PASSWORD",
    )

fun missingReleaseSigningEnvNames(): List<String> =
    releaseSigningEnvNames.filter { envName ->
        providers.environmentVariable(envName).orNull.isNullOrBlank()
    }

val releaseSigningAvailable =
    !configurationCacheRequested && missingReleaseSigningEnvNames().isEmpty()
val debugCredentialsFile = rootProject.layout.projectDirectory.file("debug.credentials.properties")
val debugCredentialsText = providers.fileContents(debugCredentialsFile).asText.orElse("")
val defaultLatencyHost = "locate.measurementlab.net"
val defaultProProductId = "runcheck_pro"
val currentReleaseVersionCode = 1
val currentReleaseVersionName = "1.0.0"
val releaseVersionCodeFloorPropertyName = "runcheck.releaseVersionCodeFloor"
val releaseVersionCodeFloorEnvName = "RUNCHECK_RELEASE_VERSION_CODE_FLOOR"
val releaseVersionCodeFloorValue =
    providers
        .gradleProperty(releaseVersionCodeFloorPropertyName)
        .orElse(providers.environmentVariable(releaseVersionCodeFloorEnvName))

fun requiredReleaseEnv(name: String): String =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: error("Release signing requires the $name environment variable.")

fun requiredReleaseVersionCodeFloor(): Int {
    val rawValue =
        releaseVersionCodeFloorValue.orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: error(
                "Release artifacts require the latest published versionCode via " +
                    "--project-prop=$releaseVersionCodeFloorPropertyName=<code> or $releaseVersionCodeFloorEnvName. " +
                    "Use 0 before the first Play upload.",
            )
    val releaseVersionCodeFloor =
        rawValue.toIntOrNull()
            ?: error("$releaseVersionCodeFloorPropertyName must be a non-negative integer.")
    require(releaseVersionCodeFloor >= 0) {
        "$releaseVersionCodeFloorPropertyName must be a non-negative integer."
    }
    return releaseVersionCodeFloor
}

fun debugCredential(
    name: String,
    vararg envNames: String,
): String {
    val localValue =
        Properties()
            .also { properties ->
                StringReader(debugCredentialsText.orNull.orEmpty()).use { properties.load(it) }
            }.getProperty(name, "")
    return envNames
        .firstNotNullOfOrNull { envName ->
            providers.environmentVariable(envName).orNull?.takeIf { it.isNotBlank() }
        }
        ?: localValue
}

fun quotedBuildConfigValue(value: String): String =
    buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> {
                    append("\\\\")
                }

                '"' -> {
                    append("\\\"")
                }

                '\n' -> {
                    append("\\n")
                }

                '\r' -> {
                    append("\\r")
                }

                '\t' -> {
                    append("\\t")
                }

                '\b' -> {
                    append("\\b")
                }

                '\u000C' -> {
                    append("\\f")
                }

                '\u2028' -> {
                    append("\\u2028")
                }

                '\u2029' -> {
                    append("\\u2029")
                }

                else -> {
                    if (character.code < 0x20 || character.code == 0x7F) {
                        append("\\u")
                        append(character.code.toString(16).padStart(4, '0'))
                    } else {
                        append(character)
                    }
                }
            }
        }
        append('"')
    }

fun validatedPlayProductId(
    envName: String,
    value: String,
): String {
    val productIdPattern = Regex("[a-z0-9][a-z0-9_.]{0,39}")
    require(productIdPattern.matches(value) && !value.startsWith("android.test")) {
        "$envName must be a Google Play product ID: 1-40 chars, start with a lowercase letter or number, " +
            "and contain only lowercase letters, numbers, underscores, or periods."
    }
    return value
}

fun validatedLatencyHost(
    envName: String,
    value: String,
): String {
    val invalidHostChars = Regex("""[\s/\\@"'\[\]]""")
    val hostPortPattern = Regex("""^[A-Za-z0-9.-]+:\d{1,5}$""")
    require(value.isNotBlank()) { "$envName must not be blank." }
    require(value.length <= 253) { "$envName must be 253 characters or fewer." }
    require(
        !value.contains("://") &&
            !invalidHostChars.containsMatchIn(value) &&
            !hostPortPattern.matches(value),
    ) {
        "$envName must be a host name or IP literal, not a URL or host:port pair."
    }
    return value
}

fun validatedLatencyPort(
    envName: String,
    defaultValue: Int,
): Int {
    val rawValue =
        providers.environmentVariable(envName).orNull?.takeIf { it.isNotBlank() }
            ?: return defaultValue
    val port =
        rawValue.toIntOrNull()
            ?: error("$envName must be an integer TCP port from 1 to 65535.")
    require(port in 1..65_535) { "$envName must be an integer TCP port from 1 to 65535." }
    return port
}

fun isReleaseArtifactTaskName(name: String): Boolean =
    name == "copyReleaseArtifacts" ||
    (
        name.endsWith("Release") &&
            (
                name.startsWith("assemble") ||
                    name.startsWith("bundle") ||
                    name.startsWith("package") ||
                    name.startsWith("publish")
            )
    ) ||
        name in
        setOf(
            "packageReleaseBundle",
            "packageReleaseUniversalApk",
            "signReleaseBundle",
        )

fun requestedTaskName(taskPath: String): String =
    taskPath.substringAfterLast(':').takeIf { it.isNotBlank() } ?: taskPath

fun isReleaseArtifactTaskRequested(): Boolean =
    gradle.startParameter.taskNames.any { taskPath ->
        isReleaseArtifactTaskName(requestedTaskName(taskPath))
    }

fun validateReleaseArtifactRequest() {
    if (configurationCacheRequested) {
        error(
            "Signed release artifact tasks must be run with --no-configuration-cache " +
                "so RUNCHECK_* signing secrets are not stored in configuration-cache entries.",
        )
    }

    val releaseVersionCodeFloor = requiredReleaseVersionCodeFloor()
    if (currentReleaseVersionCode <= releaseVersionCodeFloor) {
        error(
            "Release versionCode $currentReleaseVersionCode must be greater than " +
                "latest published versionCode $releaseVersionCodeFloor.",
        )
    }

    if (!releaseSigningAvailable) {
        error(
            "Release signing requires these missing environment variables: " +
                missingReleaseSigningEnvNames().joinToString(),
        )
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

java {
    toolchain {
        languageVersion.set(
            org.gradle.jvm.toolchain.JavaLanguageVersion
                .of(17),
        )
    }
}

android {
    namespace = "com.runcheck"
    compileSdk = 37

    sourceSets {
        getByName("androidTest") {
            assets.directories.add("$projectDir/schemas")
        }
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    androidResources {
        localeFilters += listOf("en")
    }

    defaultConfig {
        applicationId = "com.runcheck"
        minSdk = 26
        targetSdk = 37
        versionCode = currentReleaseVersionCode
        versionName = currentReleaseVersionName
        buildConfigField("String", "ROOM_DB_NAME", "\"runcheck.db\"")
        buildConfigField("String", "PRO_PRODUCT_ID", quotedBuildConfigValue(defaultProProductId))
        buildConfigField("String", "LATENCY_HOST", quotedBuildConfigValue(defaultLatencyHost))
        buildConfigField("int", "LATENCY_PORT", "443")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Set these via environment variables before release:
            // RUNCHECK_KEYSTORE_PATH, RUNCHECK_KEYSTORE_PASSWORD,
            // RUNCHECK_KEY_ALIAS, RUNCHECK_KEY_PASSWORD
            if (releaseSigningAvailable) {
                storeFile = file(requiredReleaseEnv("RUNCHECK_KEYSTORE_PATH"))
                storePassword = requiredReleaseEnv("RUNCHECK_KEYSTORE_PASSWORD")
                keyAlias = requiredReleaseEnv("RUNCHECK_KEY_ALIAS")
                keyPassword = requiredReleaseEnv("RUNCHECK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "SENTRY_DSN",
                quotedBuildConfigValue(debugCredential("sentry.dsn", "RUNCHECK_SENTRY_DSN", "SENTRY_DSN")),
            )
            val debugProProductId =
                validatedPlayProductId(
                    "RUNCHECK_PRO_PRODUCT_ID",
                    providers.environmentVariable("RUNCHECK_PRO_PRODUCT_ID").getOrElse(defaultProProductId),
                )
            buildConfigField("String", "PRO_PRODUCT_ID", quotedBuildConfigValue(debugProProductId))
            val debugLatencyHost =
                validatedLatencyHost(
                    "RUNCHECK_LATENCY_HOST",
                    providers
                        .environmentVariable(
                            "RUNCHECK_LATENCY_HOST",
                        ).getOrElse(defaultLatencyHost),
                )
            val debugLatencyPort =
                validatedLatencyPort("RUNCHECK_LATENCY_PORT", 443)
            buildConfigField("String", "LATENCY_HOST", quotedBuildConfigValue(debugLatencyHost))
            buildConfigField("int", "LATENCY_PORT", debugLatencyPort.toString())
        }
        release {
            isDebuggable = false
            isJniDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Abort release builds on errors, but allow warnings during adoption
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true

        // High-signal checks — correctness, security, performance, interop
        enable +=
            setOf(
                "NewApi", // API calls above minSdk without guards
                "InlinedApi", // Inlined constants from newer APIs
                "ObsoleteSdkInt", // SDK_INT checks that are always true given minSdk
                "UnusedResources", // Dead strings, drawables, layouts
                "MissingPermission", // API calls missing declared permissions
                "HardcodedText", // Strings not in strings.xml (localization)
                "MissingTranslation", // Incomplete translations
                "Recycle", // TypedArray/Cursor not recycled
                "StaticFieldLeak", // Context leaks in static fields
                "SetTextI18n", // Concatenated text in setText (i18n issue)
                "RtlHardcoded", // Left/right instead of start/end
                "ContentDescription", // Missing contentDescription (a11y)
                "PrivateResource", // Using private framework resources
                "InvalidPackage", // Importing packages not on Android
                "WrongThread", // UI operations off main thread
            )

        // Intentionally disabled — too noisy or not relevant
        disable +=
            setOf(
                "GradleDependency", // version bumps are manual decisions
                "AndroidGradlePluginVersion",
                "NotificationPermission", // already handled at runtime in Settings
            )

        // Don't lint generated code
        checkGeneratedSources = false

        // Write HTML + XML reports for CI/local review
        htmlReport = true
        xmlReport = true
    }
}

val releaseArtifactBaseName = "runcheck-$currentReleaseVersionName-code$currentReleaseVersionCode-release"

tasks.register<Copy>("copyReleaseArtifacts") {
    group = "distribution"
    description = "Copies release APK/AAB outputs to versioned upload filenames."
    dependsOn("assembleRelease", "bundleRelease")

    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk")) {
        rename { "$releaseArtifactBaseName.apk" }
    }
    from(layout.buildDirectory.file("outputs/bundle/release/app-release.aab")) {
        rename { "$releaseArtifactBaseName.aab" }
    }
    into(layout.buildDirectory.dir("outputs/release-upload"))
}

if (isReleaseArtifactTaskRequested()) {
    validateReleaseArtifactRequest()
}

tasks.configureEach {
    if (isReleaseArtifactTaskName(name)) {
        doFirst {
            validateReleaseArtifactRequest()
        }
    }
}

hilt {
    enableAggregatingTask = true
}

ktlint {
    version.set(libs.versions.ktlint.get())
    android.set(true)
    ignoreFailures.set(false)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("detekt-baseline.xml")
    parallel = true
}

composeStabilityAnalyzer {
    stabilityValidation {
        enabled.set(true)
        outputDir.set(layout.projectDirectory.dir("stability"))
        includeTests.set(false)
        failOnStabilityChange.set(true)
        ignoreNonRegressiveChanges.set(false)
        allowMissingBaseline.set(false)
    }
}

dependencyCheck {
    formats = listOf("HTML", "JSON", "SARIF")
    outputDirectory = rootProject.layout.projectDirectory.dir("reports")
    suppressionFile =
        rootProject.layout.projectDirectory
            .file("config/dependency-check/suppressions.xml")
            .asFile.absolutePath
    data {
        val defaultDataDirectory =
            rootProject.layout.projectDirectory
                .dir(".gradle/dependency-check-data")
                .asFile.absolutePath

        directory =
            providers
                .environmentVariable("DEPENDENCY_CHECK_DATA_DIRECTORY")
                .orElse(defaultDataDirectory)
                .get()
    }
    autoUpdate =
        providers
            .environmentVariable("DEPENDENCY_CHECK_AUTO_UPDATE")
            .map { it.equals("true", ignoreCase = true) || it == "1" || it.equals("yes", ignoreCase = true) }
            .getOrElse(true)
    failBuildOnCVSS =
        providers
            .environmentVariable("DEPENDENCY_CHECK_FAIL_BUILD_ON_CVSS")
            .map { it.toFloatOrNull() ?: 7f }
            .getOrElse(7f)
    scanConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
    skipTestGroups = true
    analyzers {
        ossIndex {
            enabled = false
        }
    }
    nvd {
        providers.environmentVariable("NVD_API_KEY").orNull?.let { apiKey = it }
        delay =
            providers
                .environmentVariable("NVD_API_DELAY_MS")
                .map { it.toIntOrNull() ?: 6_000 }
                .getOrElse(6_000)
        maxRetryCount =
            providers
                .environmentVariable("NVD_API_MAX_RETRY_COUNT")
                .map { it.toIntOrNull() ?: 20 }
                .getOrElse(20)
        validForHours =
            providers
                .environmentVariable("NVD_VALID_FOR_HOURS")
                .map { it.toIntOrNull() ?: 24 }
                .getOrElse(24)
    }
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val jacocoDebugUnitTestReportExclusions =
    listOf(
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/R.class",
        "**/R$*.class",
        "**/*Test*.*",
        "**/*Preview*.*",
        "**/*ComposableSingletons*.*",
        "**/*_Impl*.*",
        "**/*inlined*.*",
        "**/di/**",
        "**/RuncheckApp*.*",
        "**/MainActivity*.*",
        "**/SentryInit*.*",
        "**/data/appusage/AppUsageDataSource*.*",
        "**/data/battery/Android14BatterySource*.*",
        "**/data/battery/BatteryDataSourceFactory*.*",
        "**/data/billing/BillingManager*.*",
        "**/data/network/LatencyMeasurer*.*",
        "**/data/network/NetworkDataSource*.*",
        "**/data/storage/CleanupThumbnailLoader*.*",
        "**/data/storage/MediaStoreScanner*.*",
        "**/data/storage/StorageCleanupHelper*.*",
        "**/data/storage/StorageDataSource*.*",
        "**/pro/TrialManager*.*",
        "**/service/monitor/*Service*.*",
        "**/service/monitor/*Worker*.*",
        "**/service/monitor/*Receiver*.*",
        "**/service/monitor/NotificationHelper*.*",
        "**/ui/**/*Screen*.*",
        "**/ui/**/*Sections*.*",
        "**/ui/**/*Section*.*",
        "**/ui/**/*Card*.*",
        "**/ui/**/*Row*.*",
        "**/ui/**/*Sheet*.*",
        "**/ui/**/*Dialog*.*",
        "**/ui/**/*Content*.*",
        "**/ui/**/*Message*.*",
        "**/ui/**/*Chip*.*",
        "**/ui/**/*Group*.*",
        "**/ui/**/*Support*.*",
        "**/ui/**/*Modal*.*",
        "**/ui/**/*Overlay*.*",
        "**/ui/**/*Badge*.*",
        "**/ui/**/*Pill*.*",
        "**/ui/**/NavGraph*.*",
        "**/ui/common/ChartSelection*.*",
        "**/ui/common/LifecycleStartStopEffect*.*",
        "**/ui/home/insights/InsightStringResolver*.*",
        "**/ui/storage/cleanup/CleanupBottomBar*.*",
        "**/ui/storage/cleanup/FileListItem*.*",
        "**/ui/components/*Chart*.*",
        "**/ui/components/AnimatedNumber*.*",
        "**/ui/components/HeatStrip*.*",
        "**/ui/components/MiniBar*.*",
        "**/ui/components/ProFeatureLockedState*.*",
        "**/ui/components/ProgressRing*.*",
        "**/ui/components/SegmentedBar*.*",
        "**/ui/components/SegmentedStatusBar*.*",
        "**/ui/components/SignalBars*.*",
        "**/ui/theme/**",
        "**/widget/**",
    )

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    group = "verification"
    description = "Luo JaCoCo XML -raportin SonarCloudin debug unit test -coveragea varten."

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml"),
        )
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoDebugUnitTestReport/html"))
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(
            fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
                exclude(jacocoDebugUnitTestReportExclusions)
            },
            fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
                exclude(jacocoDebugUnitTestReportExclusions)
            },
            fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
                exclude(jacocoDebugUnitTestReportExclusions)
            },
        ),
    )
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
            )
        },
    )
}

val roomSerializationConstraintReason = "Room migration helpers require aligned kotlinx.serialization artifacts"

dependencies {
    constraints {
        implementation(libs.kotlin.stdlib.jdk7) {
            because("Keep Kotlin JDK adapter artifacts aligned with the resolved Kotlin runtime line")
        }
        implementation(libs.kotlin.stdlib.jdk8) {
            because("Keep Kotlin JDK adapter artifacts aligned with the resolved Kotlin runtime line")
        }
        implementation(libs.kotlinx.serialization.core) {
            because(roomSerializationConstraintReason)
        }
        implementation(libs.kotlinx.serialization.json) {
            because(roomSerializationConstraintReason)
        }
        implementation(libs.lifecycle.viewmodel.compose) {
            because("Keep transitive Lifecycle Compose artifacts aligned with the verified lifecycle version")
        }
        androidTestImplementation(libs.kotlinx.serialization.core) {
            because(roomSerializationConstraintReason)
        }
        androidTestImplementation(libs.kotlinx.serialization.json) {
            because(roomSerializationConstraintReason)
        }
    }

    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Hilt WorkManager
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // WorkManager
    implementation(libs.workmanager)

    // DataStore
    implementation(libs.datastore.preferences)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    // Baseline Profile
    runtimeOnly(libs.profileinstaller)

    // Gson
    implementation(libs.gson)

    // M-Lab NDT7 speed test
    implementation(libs.ndt7)
    // NDT7 references OkHttp APIs, but the JitPack POM does not declare them.
    implementation(libs.okhttp)

    // Google Play Billing
    implementation(libs.billing)

    // Glance (home screen widgets)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Sentry on vain debug-diagnostiikkaa. Release-luokkapolku tarkistetaan tools\sentry.ps1-komennolla.
    debugImplementation(libs.sentry.android.core)

    // Static analysis plugins
    detektPlugins(libs.compose.rules.detekt)
    ktlintRuleset(libs.compose.rules.ktlint)
    lintChecks(libs.android.security.lints)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
}
