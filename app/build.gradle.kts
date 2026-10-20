plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.satya.ramayana"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.satya.ramayana"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "2.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}