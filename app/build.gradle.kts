plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val persistentKeystorePath = providers.environmentVariable("W2FULL_DEBUG_KEYSTORE_PATH").orNull

android {
    namespace = "com.archimede.w2full"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.archimede.w2full"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.3.0-m4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (!persistentKeystorePath.isNullOrBlank()) {
        signingConfigs {
            create("persistentDebug") {
                storeFile = file(persistentKeystorePath)
                storePassword = providers.environmentVariable("W2FULL_DEBUG_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("W2FULL_DEBUG_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("W2FULL_DEBUG_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        debug {
            if (!persistentKeystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("persistentDebug")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    val roomVersion = "2.8.4"
    val lifecycleVersion = "2.11.0"
    val okhttpVersion = "5.5.0"

    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("com.squareup.okhttp3:mockwebserver3:$okhttpVersion")
}
