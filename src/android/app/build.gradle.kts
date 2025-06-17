plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.texttohandwriting"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.texttohandwriting"
        minSdk = 26
        targetSdk = 35
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Add SSP & SDP dependencies
    implementation("com.intuit.ssp:ssp-android:1.1.1")
    implementation("com.intuit.sdp:sdp-android:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // For JSON conversion, for example using Gson:
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Optional: add OkHttp logging interceptor for debugging
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    implementation("com.google.code.gson:gson:2.10.1")

}