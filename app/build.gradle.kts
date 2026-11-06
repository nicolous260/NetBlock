plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.nicolous260.netblock"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.nicolous260.netblock"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
    }

    // Release signing comes from the environment so no key ever lives in the repo.
    // Without it the release build falls back to the debug key (installable, but not for distribution).
    val keystorePath = System.getenv("NETBLOCK_KEYSTORE")
    val hasReleaseKey = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("NETBLOCK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NETBLOCK_KEY_ALIAS")
                keyPassword = System.getenv("NETBLOCK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "release" else "debug")
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
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
