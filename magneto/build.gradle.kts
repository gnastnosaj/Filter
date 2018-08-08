import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.project

plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    val compileSdkVersion: Int by rootProject.ext
    val buildToolsVersion: String by rootProject.ext
    val minSdkVersion: Int by rootProject.ext
    val targetSdkVersion: Int by rootProject.ext
    val versionCode: Int by rootProject.ext
    val versionName: String by rootProject.ext

    compileSdkVersion(compileSdkVersion)
    buildToolsVersion(buildToolsVersion)

    defaultConfig {
        minSdkVersion(minSdkVersion)
        targetSdkVersion(targetSdkVersion)
        this.versionCode = versionCode
        this.versionName = versionName
    }

    buildTypes {
        getByName("debug") {
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }
}

val kotlinVersion: String by rootProject.ext

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(kotlin("stdlib", kotlinVersion))

    implementation("io.reactivex.rxjava2:rxjava:+")
    implementation("io.reactivex.rxjava2:rxandroid:+")
    implementation("com.squareup.retrofit2:retrofit:+")
    implementation("com.squareup.retrofit2:adapter-rxjava2:+")
    implementation("com.squareup.retrofit2:converter-gson:+")
    implementation("com.squareup.okhttp3:okhttp:+")
    implementation("com.squareup.okhttp3:logging-interceptor:+")

    compileOnly(project(":dsl"))
    compileOnly(project(":dsl-groovy"))
}