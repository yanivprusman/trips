import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("android-flavors")
}

val gitCommitCount = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.get().trim().toIntOrNull() ?: 1

val gitShortHash = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().trim().ifEmpty { "dev" }

// Local, gitignored build config (mobile/.env): backend base URL baked at build time.
val envFile = rootProject.file(".env")
val envProps = Properties()
if (envFile.exists()) envFile.inputStream().use { envProps.load(it) }
val apiBaseUrl = envProps.getProperty("API_BASE_URL", "http://10.7.0.2:3157/")

android {
    namespace = "com.automatelinux.trips"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.automatelinux.trips"
        minSdk = 26
        targetSdk = 35
        versionCode = gitCommitCount
        versionName = "v${gitCommitCount} (${gitShortHash})"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // One APK per ABI: the phone takes arm64-v8a, the emulator x86_64. MapLibre's native
    // library is the bulk of the APK, and a universal one carried four copies of it over
    // WireGuard for nothing.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }
    androidResources {
        // The map tiles and photos are already compressed images and the trip file is
        // compact JSON: storing them uncompressed lets MapLibre and the loader read them
        // straight out of the APK instead of inflating them on every access.
        noCompress += listOf("json", "png", "jpg")
    }
}

dependencies {
    // Shared KMP module (commonMain code shared with iOS)
    implementation(project(":shared"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.multiplatform.settings)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit/Gson/OkHttp exist for feedback-lib's FeedbackApi; the app itself has no network client.
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)

    // The map
    implementation(libs.maplibre)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    "devImplementation"(project(":feedback-lib"))
}
