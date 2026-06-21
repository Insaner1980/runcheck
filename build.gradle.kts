plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
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
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            rootProject.layout.projectDirectory
                .file("app/build/reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml")
                .asFile.absolutePath,
        )
    }
}

tasks.named("sonar") {
    dependsOn(":app:assembleDebug", ":app:jacocoDebugUnitTestReport")
}
