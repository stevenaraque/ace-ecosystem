plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "sena.adso.ace_wear"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "sena.adso.ace_wear"
        minSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Wear OS: Health Services (obligatorio para A.C.E)
    implementation(libs.health.services.client)

    // Wear OS: Data Layer
    implementation(libs.play.services.wearable)

    // Compose BOM
    implementation(platform(libs.compose.bom))

    // Compose UI (versiones del BOM)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    // Wear Compose (NO usar material3 de móvil)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material)
    // implementation(libs.wear.compose.material3) // Solo si necesitas Material3 experimental

    // Tooling
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
}