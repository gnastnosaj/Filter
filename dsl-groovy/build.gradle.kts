plugins {
    id("com.android.library")
    id("groovyx.android")
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

    lintOptions {
        isAbortOnError = false
    }
}

dependencies {
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    compileOnly(project(":dsl"))
}