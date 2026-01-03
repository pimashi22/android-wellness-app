plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.moodee"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.moodee"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ✅ JSON persistence
    implementation("com.google.code.gson:gson:2.10.1")

    // ✅ MPAndroidChart for charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")


    // ✅ (Optional but recommended) RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ✅ (Optional) WorkManager if you do hydration reminders with WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("androidx.cardview:cardview:1.0.0")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}