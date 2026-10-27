plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "es.upm.miw.chefpocket"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "es.upm.miw.chefpocket"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Firebase (BoM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.ui.auth)

    // Retrofit + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
