import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.onesignal)
}

android {
    namespace = "com.example.arcadia"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.arcadia"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Load properties from local.properties and expose API Keys via BuildConfig
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            val localProperties = Properties().apply {
                load(localPropertiesFile.inputStream())
            }
            val rawgApiKey = localProperties.getProperty("RAWG_API_KEY", "")
            val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY", "")
            val groqApiKey = localProperties.getProperty("GROQ_API_KEY", "")
            val onesignalAppId = localProperties.getProperty("ONESIGNAL_APP_ID", "")
            val onesignalRestApiKey = localProperties.getProperty("ONESIGNAL_REST_API_KEY", "")
            buildConfigField("String", "RAWG_API_KEY", "\"$rawgApiKey\"")
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
            buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
            buildConfigField("String", "ONESIGNAL_APP_ID", "\"$onesignalAppId\"")
            buildConfigField("String", "ONESIGNAL_REST_API_KEY", "\"$onesignalRestApiKey\"")
        } else {
            buildConfigField("String", "RAWG_API_KEY", "\"\"")
            buildConfigField("String", "GEMINI_API_KEY", "\"\"")
            buildConfigField("String", "GROQ_API_KEY", "\"\"")
            buildConfigField("String", "ONESIGNAL_APP_ID", "\"\"")
            buildConfigField("String", "ONESIGNAL_REST_API_KEY", "\"\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // Firebase (🔥)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    //Icons
    implementation(libs.androidx.compose.material.icons.extended)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.gif)

    // Navigation
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Splash screen
    implementation(libs.androidx.core.splashscreen)

    //coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    //video playback
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)


    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    
    // Reorderable - drag and drop for LazyColumn/LazyVerticalGrid
    implementation("sh.calvin.reorderable:reorderable:2.4.0")

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.brotli)
    implementation(libs.kotlinx.serialization.converter)
    implementation(libs.okhttp.logging.interceptor)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Gemini AI
    implementation(libs.generativeai)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // Paging 3
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // QR Code - ZXing for generation
    implementation(libs.zxing.core)

    // ML Kit Barcode Scanning
    implementation(libs.mlkit.barcode.scanning)

    // CameraX for QR scanning
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // OneSignal Push Notifications
    implementation(libs.onesignal)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
