plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.legacy.kapt)        // ← NUEVO: com.android.legacy-kapt
    alias(libs.plugins.hilt.android)       // ← NUEVO
}

android {
    namespace = "sena.adso.ace_wear"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                it.jvmArgs("-XX:+EnableDynamicAgentLoading")
            }
        }
    }


    defaultConfig {
        applicationId = "sena.adso.ace"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.ace.shared)
    // Kotlin & Corrutinas
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Wear OS Core
    implementation(libs.core.ktx)
    implementation(libs.wear)

    // Health & Data
    implementation(libs.health.services.client)
    implementation(libs.play.services.wearable)

    // Compose (BOM)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    // Wear Compose
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material)

    // Lifecycle (MVVM)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.service)

    // Hilt (DI) — usando legacy-kapt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)               // ← kapt() funciona con legacy-kapt

    // Tooling
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.kotlin)


}