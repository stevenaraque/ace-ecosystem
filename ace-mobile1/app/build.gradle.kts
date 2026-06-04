plugins {
    alias(libs.plugins.android.application)
    // kotlin-android ELIMINADO — AGP 9.0 lo incluye de forma integrada.
    // kotlin-compose SÍ es necesario explícitamente desde Kotlin 2.0
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "sena.adso.ace_mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "sena.adso.ace_mobile"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // En AGP 9.0 con Kotlin integrado, kotlinOptions se reemplaza por kotlin {}
    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
    }

    // composeOptions ya no es necesario con Kotlin Compose integrado en AGP 9.0
    // El compilador de Compose lo gestiona el plugin kotlin.compose automáticamente

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- Core ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // --- A.C.E: Compose ViewModel + Lifecycle ---
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // --- A.C.E: Navigation Compose ---
    implementation(libs.navigation.compose)

    // --- A.C.E: Wear OS Data Layer ---
    implementation(libs.play.services.wearable)

    // --- A.C.E: Room ---
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // --- A.C.E: WorkManager ---
    implementation(libs.work.manager)

    // --- A.C.E: Retrofit + OkHttp ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // --- A.C.E: Hilt ---
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.work.compiler)

    // --- A.C.E: DataStore ---
    implementation(libs.datastore.preferences)

    // --- A.C.E: Coroutines ---
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // --- Test ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}