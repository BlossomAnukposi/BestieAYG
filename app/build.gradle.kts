import java.util.Properties

plugins {
plugins {
    alias(libs.plugins.android.application)
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlin.plugin.compose")
}

// Load local.properties to read API key securely
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

android {
    namespace = "com.bayg"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.bayg"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject API key into BuildConfig (if missing, use empty string to avoid build failures)
        val apiKey = localProps.getProperty("OPENWEATHER_API_KEY") ?: ""
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"$apiKey\"")
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.activity:activity-compose:1.9.0")    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines (required for suspend functions)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}