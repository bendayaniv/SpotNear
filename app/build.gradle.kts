plugins {
    alias(libs.plugins.android.application)
//    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.spotnear"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.spotnear"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.github.bendayaniv:LocationLibrary:1.00.05")

    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.gms:play-services-location:18.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")

    // Fragment
    val fragment_version = "1.6.1"
    //noinspection GradleDependency
    implementation("androidx.fragment:fragment:$fragment_version")
}