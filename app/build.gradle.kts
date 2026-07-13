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
        // F-Droid clients installed the Flutter app at versionCode 233 (1.5.1 arm64 split);
        // the Kotlin rewrite ships a single universal APK, so this must stay above that.
        versionCode = 240
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    androidResources {
        localeFilters += setOf("en", "de", "it", "b+lld")
    }

    // Room writes a JSON snapshot of every schema version here (committed to git);
    // these are what MigrationTestHelper replays to test future migrations.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    // Expose the committed schema JSONs to MigrationTestHelper as instrumentation-test assets.
    sourceSets {
        getByName("androidTest") {
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }

    signingConfigs {
        // Used by the release CI workflow; falls back to an unsigned build locally.
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
        // Google Play's encrypted dependency block makes the APK/AAB differ from what anyone
        // else can build from source; F-Droid requires it off for reproducible builds.
        includeInApk = false
        includeInBundle = false
    }
    buildTypes {
        release {
            vcsInfo {
                // AGP embeds META-INF/version-control-info.textproto by default; any difference
                // in git state between CI and the F-Droid builder breaks reproducible builds.
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
            // The app switches language at runtime (LocaleHelper); keep all locales in every
            // install instead of relying on Play's on-demand language downloads.
            enableSplit = false
        }
    }
    lint {
        // Translations are managed on Crowdin and land asynchronously; a key missing from one
        // locale falls back to English at runtime and must not fail the build.
        warning += "MissingTranslation"
        // Renovate owns dependency updates; lint re-reporting newer versions is just noise
        // that accumulates until the next Renovate merge.
        disable += listOf("GradleDependency", "NewerVersionAvailable")
        // Pre-existing findings (mostly compose-lints style rules on code that predates them)
        // are frozen here; lint only fails on issues introduced afterwards. Regenerate with:
        // rm app/lint-baseline.xml && ./gradlew :app:lintDebug
        baseline = file("lint-baseline.xml")
        // SARIF is uploaded to GitHub code scanning by the lint workflow.
        sarifReport = true
    }
    packaging {
        jniLibs {
            keepDebugSymbols.add("**/*.so")
        }
    }
}

// room-testing 2.8.4 parses the schema JSONs with kotlinx-serialization 1.8.x, but
// navigation-compose pins the whole group to 1.7.3 (strictly), which crashes MigrationTestHelper
// with an AbstractMethodError. Force the newer, backward-compatible version for the test classpath.
configurations.matching { it.name.contains("AndroidTest") }.configureEach {
    val serializationVersion = libs.versions.kotlinxSerializationForRoomTesting.get()
    resolutionStrategy.force(
        "org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion",
        "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$serializationVersion",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion",
        "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$serializationVersion",
    )
}

detekt {
    // Existing findings live in the baseline; detekt only fails on issues introduced after it.
    // Regenerate with: ./gradlew :app:detektBaseline
    baseline = file("detekt-baseline.xml")
    // Default rules plus the Compose adjustments in config/detekt/detekt.yml.
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        // Uploaded to GitHub code scanning by the CI workflow, alongside Android Lint's.
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

    // Slack's Compose best-practice rules, picked up by the normal lint task.
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
    // Automatic Activity/Service leak detection in debug builds; ships nothing in release.
    debugImplementation(libs.leakcanary.android)
}
