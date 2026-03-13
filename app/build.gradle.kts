plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.devicepulse"
    compileSdk = 36

    androidResources {
        localeFilters += listOf("en", "fi")
    }

    defaultConfig {
        applicationId = "com.devicepulse"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        val proProductId = System.getenv("DEVICEPULSE_PRO_PRODUCT_ID") ?: "devicepulse_pro"
        val latencyHost = System.getenv("DEVICEPULSE_LATENCY_HOST") ?: "locate.measurementlab.net"
        val latencyPort = (System.getenv("DEVICEPULSE_LATENCY_PORT") ?: "443").toIntOrNull() ?: 443
        buildConfigField("String", "PRO_PRODUCT_ID", "\"$proProductId\"")
        buildConfigField("String", "LATENCY_HOST", "\"$latencyHost\"")
        buildConfigField("int", "LATENCY_PORT", latencyPort.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Set these via environment variables before release:
            // DEVICEPULSE_KEYSTORE_PATH, DEVICEPULSE_KEYSTORE_PASSWORD,
            // DEVICEPULSE_KEY_ALIAS, DEVICEPULSE_KEY_PASSWORD
            val keystorePath = System.getenv("DEVICEPULSE_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("DEVICEPULSE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DEVICEPULSE_KEY_ALIAS")
                keyPassword = System.getenv("DEVICEPULSE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val keystorePath = System.getenv("DEVICEPULSE_KEYSTORE_PATH")
            if (keystorePath != null) {
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
    parallel = true
}

tasks.configureEach {
    if (name.startsWith("hiltJavaCompile") && name.endsWith("UnitTest")) {
        enabled = false
    }
}

configurations.configureEach {
    resolutionStrategy.activateDependencyLocking()
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
    implementation(libs.room.ktx)
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

    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    // Gson
    implementation(libs.gson)

    // Firebase Crashlytics
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    // M-Lab NDT7 speed test
    implementation(libs.ndt7)

    // Google Play Billing
    implementation(libs.billing)

    // Glance (home screen widgets)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
}
