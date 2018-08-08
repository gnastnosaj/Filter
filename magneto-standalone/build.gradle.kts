plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android-extensions")
}

android {
    val compileSdkVersion: Int by rootProject.ext
    val buildToolsVersion: String by rootProject.ext
    val minSdkVersion: Int by rootProject.ext
    val targetSdkVersion: Int by rootProject.ext

    compileSdkVersion(compileSdkVersion)
    buildToolsVersion(buildToolsVersion)

    defaultConfig {
        applicationId = "com.github.gnastnosaj.filter.magneto.standalone"

        minSdkVersion(minSdkVersion)
        targetSdkVersion(targetSdkVersion)
        versionCode = 1
        versionName = "0.0.1"

        multiDexEnabled = true
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

    packagingOptions {
        exclude("**/*.kotlin_builtins")
        exclude("**/*.kotlin_module")
        exclude("okhttp3/internal/publicsuffix/publicsuffixes.gz")
    }
}

val kotlinVersion: String by rootProject.ext
val supportLibraryVersion: String by rootProject.ext
val ankoVersion: String by rootProject.ext
val boilerplateVersion: String by rootProject.ext

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(kotlin("stdlib", kotlinVersion))

    implementation("com.android.support:multidex:1.0.3")

    implementation("org.jetbrains.anko:anko-commons:$ankoVersion")
    implementation("org.jetbrains.anko:anko-sdk25:$ankoVersion")
    implementation("org.jetbrains.anko:anko-appcompat-v7-commons:$ankoVersion")
    implementation("org.jetbrains.anko:anko-appcompat-v7:$ankoVersion")
    implementation("org.jetbrains.anko:anko-design:$ankoVersion")
    implementation("org.jetbrains.anko:anko-recyclerview-v7:$ankoVersion")

    implementation("com.github.gnastnosaj.Boilerplate:boilerplate:$boilerplateVersion") {
        exclude(mapOf("group" to "com.facebook.fresco"))
    }
    implementation("com.github.gnastnosaj.Boilerplate:net:$boilerplateVersion")
    implementation("com.github.gnastnosaj.okhttp:okhttp-urlconnection:dafbeaf") {
        exclude(mapOf("module" to "okhttp"))
    }
    implementation("com.squareup.okhttp3:logging-interceptor:+")
    implementation("com.tbruyelle.rxpermissions2:rxpermissions:+@aar")
    implementation("com.github.VictorAlbertos.RxCache:runtime:+")
    implementation("com.github.VictorAlbertos.Jolyglot:gson:+")

    implementation("com.mikepenz:material-design-iconic-typeface:+@aar")

    implementation("com.github.gnastnosaj:MaterialSearchView:4463cd3949")
    implementation("com.yqritc:recyclerview-flexibledivider:+")

    implementation(project(":dsl"))
    implementation(project(":dsl-groovy"))
    implementation(project(":magneto"))
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.android.support" && !requested.name.startsWith("multidex")) {
            useVersion(supportLibraryVersion)
        }
    }
}