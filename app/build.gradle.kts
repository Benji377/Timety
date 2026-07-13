plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

android {
    namespace = "io.github.benji377.timety"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.benji377.timety"
        minSdk = 26
        targetSdk = 37
        // Must stay above 233, the last Flutter-era versionCode on F-Droid.
        versionCode = 241
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    androidResources {
        localeFilters += setOf("en", "de", "it", "b+lld")
    }

    // Committed Room schemas as test assets for MigrationTestHelper.
    sourceSets {
        getByName("androidTest") {
            assets.directories.add("$projectDir/schemas")
        }
    }

    signingConfigs {
        // Set by the release CI workflow; local builds stay unsigned.
        val keystorePath = System.getenv("KEYSTORE_PATH")
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    dependenciesInfo {
        // Play's encrypted dependency block breaks F-Droid reproducible builds.
        includeInApk = false
        includeInBundle = false
    }
    buildTypes {
        release {
            vcsInfo {
                // Embedded git state breaks F-Droid reproducible builds.
                include = false
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    bundle {
        language {
            // Runtime language switching needs every locale in the install.
            enableSplit = false
        }
    }
    lint {
        // Crowdin translations land asynchronously; missing keys fall back to English.
        warning += "MissingTranslation"
        // Renovate owns dependency updates.
        disable += listOf("GradleDependency", "NewerVersionAvailable")
        // compose-lints rules meant for published libraries, not app-internal screens.
        disable += listOf(
            "ComposeModifierMissing",
            "ComposeParameterOrder",
            "ComposeViewModelForwarding",
            "ComposeMultipleContentEmitters",
            "SlotReused",
            "ComposeCompositionLocalUsage",
        )
        // SARIF is uploaded to GitHub code scanning by the lint workflow.
        sarifReport = true
    }
    packaging {
        jniLibs {
            keepDebugSymbols.add("**/*.so")
        }
    }
}

// Room schema snapshots (committed), replayed by MigrationTestHelper.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// navigation-compose strictly pins kotlinx-serialization 1.7.3, which crashes
// room-testing's schema parser; force the newer version on the test classpath.
configurations.matching { it.name.contains("AndroidTest") }.configureEach {
    resolutionStrategy.force(
        "org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0",
        "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.11.0",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0",
        "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0",
    )
}

detekt {
    // Default rules; per-rule opt-outs and Compose tweaks live in the config file.
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        // Uploaded to GitHub code scanning by CI.
        sarif.required.set(true)
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Glance
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    androidTestImplementation(libs.room.testing)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coil for SVG
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // Slack's Compose rules for the lint task.
    lintChecks(libs.compose.lint.checks)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    // Leak detection in debug builds only.
    debugImplementation(libs.leakcanary.android)
}
