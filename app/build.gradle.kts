plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
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

    // Room writes a JSON snapshot of every schema version here (committed to git);
    // these are what MigrationTestHelper replays to test future migrations.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
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
        // SARIF is uploaded to GitHub code scanning by the lint workflow.
        sarifReport = true
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Glance
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.8")

    // Room
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // Coil for SVG
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
