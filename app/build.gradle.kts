import java.util.Properties

plugins {
    jacoco
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val versionProperties =
    Properties().apply {
        rootProject.file("version.properties").inputStream().use { load(it) }
    }

// Release signing comes only from the environment, so no key ever sits in the repo.
val releaseKeystorePath: String? = System.getenv("DWB_KEYSTORE_PATH")

android {
    namespace = "io.github.greavemrsvg.dogwalkbuddy"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.greavemrsvg.dogwalkbuddy"
        minSdk = 29
        targetSdk = 36
        versionCode = versionProperties.getProperty("versionCode").toInt()
        versionName = versionProperties.getProperty("versionName")
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("DWB_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DWB_KEY_ALIAS")
                keyPassword = System.getenv("DWB_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Robolectric needs merged resources and the merged manifest.
            isIncludeAndroidResources = true
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        // Versions are pinned by ADR 0001 and bumped through Dependabot and ADR amendments. These checks
        // compare against the newest release online, so they would fail the build on an unrelated day.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Room exports its schema JSON here (ADR 0004). The files are committed.
ksp {
    arg("room.schemaLocation", "${layout.projectDirectory}/schemas")
}

jacoco {
    toolVersion = "0.8.13"
}

// Count classes that Robolectric loads from no known location (ADR 0001, coverage).
tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

// Coverage report with the exclusions of ADR 0001, "Coverage exclusions". Nothing else is excluded.
val coverageExcludes =
    listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/*_Impl*.*",
        "**/*ComposableSingletons*.*",
        "**/*\$serializer.*",
        "**/map/maplibre/**",
    )

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    group = "verification"
    description = "Line coverage of the debug unit tests, with the ADR 0001 exclusions."
    dependsOn("testDebugUnitTest")
    val buildDir = layout.buildDirectory
    executionData.from(buildDir.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"))
    classDirectories.from(
        listOf(
            buildDir.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"),
            buildDir.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes"),
        ).map { dir -> fileTree(dir) { exclude(coverageExcludes) } },
    )
    sourceDirectories.from(layout.projectDirectory.dir("src/main/java"))
    reports {
        xml.required = true
        xml.outputLocation = buildDir.file("reports/jacoco/jacocoDebugUnitTestReport/report.xml")
        html.required = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.maplibre.android.sdk)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit.ktx)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.okhttp.mockwebserver3.junit4)
    testImplementation(libs.okhttp.tls)
}
