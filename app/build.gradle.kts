plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.atjaa.myapplication"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.atjaa.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.2.2"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    viewBinding { enable = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.toasty)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("io.ktor:ktor-server-core:3.0.0")
    implementation("io.ktor:ktor-server-cio:3.0.0")
    implementation("io.ktor:ktor-server-host-common:3.0.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    val cameraxVersion = "1.4.0" // 摄像头操作
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

}