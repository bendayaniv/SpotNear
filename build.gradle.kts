//buildscript {
//    dependencies {
//        classpath("com.google.gms:google-services:4.4.1")
//    }
//}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.android.library") version "7.3.1" apply false
//    id("com.google.gms.google-services") version "4.4.1" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}