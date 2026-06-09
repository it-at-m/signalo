plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "de.muenchen.appcenter.signalo"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.muenchen.appcenter.signalo"
        minSdk = 31
        targetSdk = 36
        versionCode = 11
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.timber)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(project(":gaugelibrary"))
    implementation(libs.androidx.navigation.fragment.ktx.v285)
    implementation(libs.androidx.navigation.ui.ktx.v285)
    implementation(libs.material.v1130)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}