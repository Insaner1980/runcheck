plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

val releaseSigningRequested =
    gradle.startParameter.taskNames.any {
        it.contains("release", ignoreCase = true)
    }

fun requiredReleaseEnv(name: String): String =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: error("Release signing requires the $name environment variable.")

android {
    namespace = "com.runcheck"
    compileSdkPreview = "CinnamonBun"

    androidResources {
        localeFilters += listOf("en")
    }

    defaultConfig {
        applicationId = "com.runcheck"
        minSdk = 26
        targetSdkPreview = "CinnamonBun"
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "ROOM_DB_NAME", "\"runcheck.db\"")
        val proProductId = providers.environmentVariable("RUNCHECK_PRO_PRODUCT_ID").getOrElse("runcheck_pro")
        val latencyHost = providers.environmentVariable("RUNCHECK_LATENCY_HOST").getOrElse("locate.measurementlab.net")
        val latencyPort =
            providers
                .environmentVariable("RUNCHECK_LATENCY_PORT")
                .map { it.toIntOrNull() ?: 443 }
                .getOrElse(443)
        buildConfigField("String", "PRO_PRODUCT_ID", "\"$proProductId\"")
        buildConfigField("String", "LATENCY_HOST", "\"$latencyHost\"")
        buildConfigField("int", "LATENCY_PORT", latencyPort.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Set these via environment variables before release:
            // RUNCHECK_KEYSTORE_PATH, RUNCHECK_KEYSTORE_PASSWORD,
            // RUNCHECK_KEY_ALIAS, RUNCHECK_KEY_PASSWORD
            if (releaseSigningRequested) {
                storeFile = file(requiredReleaseEnv("RUNCHECK_KEYSTORE_PATH"))
                storePassword = requiredReleaseEnv("RUNCHECK_KEYSTORE_PASSWORD")
                keyAlias = requiredReleaseEnv("RUNCHECK_KEY_ALIAS")
                keyPassword = requiredReleaseEnv("RUNCHECK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "ROOM_DEBUG_TOOLS_ENABLED", "true")
            buildConfigField(
                "String",
                "SENTRY_DSN",
                "\"https://34bc2ad48c87a2c7a666076de44cf0ae@o4511121418878976.ingest.de.sentry.io/4511121470193744\"",
            )
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "ROOM_DEBUG_TOOLS_ENABLED", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningRequested) {
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
                "OldTargetApi", // targeting CinnamonBun preview
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

hilt {
    enableAggregatingTask = true
}

ktlint {
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

tasks.configureEach {
    if (name.startsWith("hiltJavaCompile") && name.endsWith("UnitTest")) {
        enabled = false
    }
}

dependencies {
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
    implementation(libs.lifecycle.viewmodel.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

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
    implementation(libs.profileinstaller)

    // Gson
    implementation(libs.gson)

    // M-Lab NDT7 speed test
    implementation(libs.ndt7)
    implementation(libs.okhttp)

    // Google Play Billing
    implementation(libs.billing)

    // Glance (home screen widgets)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Sentry (debug builds only — not shipped in release)
    debugImplementation(libs.sentry.android)

    // Detekt plugins
    detektPlugins(libs.detekt.compose.rules)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
}
