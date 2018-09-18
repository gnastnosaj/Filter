import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.text.SimpleDateFormat
import java.util.Date

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
    val versionCode: Int by rootProject.ext
    val versionName: String by rootProject.ext

    compileSdkVersion(compileSdkVersion)
    buildToolsVersion(buildToolsVersion)

    defaultConfig {
        applicationId = "com.github.gnastnosaj.filter.kaleidoscope"

        minSdkVersion(minSdkVersion)
        targetSdkVersion(targetSdkVersion)
        this.versionCode = versionCode
        this.versionName = versionName

        multiDexEnabled = true

        ndk {
            abiFilters("armeabi-v7a", "x86")
        }
    }

    buildTypes {
        getByName("debug") {
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")

            applicationVariants.all {
                outputs.filter {
                    it.outputFile.name.endsWith(".apk")
                }.forEach {
                    (it as BaseVariantOutputImpl).outputFileName = "Filter_v${defaultConfig.versionName}_${SimpleDateFormat("yyyy-MM-dd").format(Date())}_${productFlavors[0].name}.apk"
                }
            }
        }
    }

    flavorDimensions("version")

    productFlavors {
        create("pgyer") {
            setDimension("version")
            buildConfigField("String", "SHARE_URI", "\"https://www.pgyer.com/D7r4\"")
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
    implementation("com.android.support:appcompat-v7:$supportLibraryVersion")
    implementation("com.android.support:cardview-v7:$supportLibraryVersion")

    implementation("com.google.code.gson:gson:+")
    implementation("com.squareup.retrofit2:converter-gson:+")

    implementation("org.jetbrains.anko:anko-commons:$ankoVersion")
    implementation("org.jetbrains.anko:anko-sdk25:$ankoVersion")
    implementation("org.jetbrains.anko:anko-appcompat-v7-commons:$ankoVersion")
    implementation("org.jetbrains.anko:anko-appcompat-v7:$ankoVersion")
    implementation("org.jetbrains.anko:anko-design:$ankoVersion")
    implementation("org.jetbrains.anko:anko-recyclerview-v7:$ankoVersion")
    implementation("org.jetbrains.anko:anko-cardview-v7:$ankoVersion")
    implementation("org.jetbrains.anko:anko-percent:$ankoVersion")

    implementation("com.github.gnastnosaj.Boilerplate:boilerplate:$boilerplateVersion")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:+")
    releaseImplementation("com.squareup.leakcanary:leakcanary-android-no-op:+")
    implementation("com.github.JiongBull:jlog:+")
    implementation("me.drakeet.multitype:multitype-kotlin:+")
    implementation("com.github.gnastnosaj.Boilerplate:net:$boilerplateVersion")
    implementation("com.github.gnastnosaj.okhttp:okhttp-urlconnection:dafbeaf") {
        exclude(mapOf("module" to "okhttp"))
    }
    implementation("com.github.gnastnosaj.BiliShare:library:bad078d")
    implementation("com.squareup.okhttp3:logging-interceptor:+")
    implementation("io.reactivex.rxjava2:rxkotlin:+")
    implementation("com.tbruyelle.rxpermissions2:rxpermissions:+@aar")
    implementation("com.github.VictorAlbertos.RxCache:runtime:+")
    implementation("com.github.VictorAlbertos.Jolyglot:gson:+")

    implementation("com.github.gnastnosaj.Boilerplate-util:keyboard:ba6e131")
    implementation("com.github.gnastnosaj.Boilerplate-util:textdrawable:ba6e131")

    implementation("com.mikepenz:iconics-views:+@aar")
    implementation("com.mikepenz:material-design-iconic-typeface:+@aar")

    implementation("com.github.gnastnosaj:MaterialSearchView:4463cd3949")
    implementation("com.github.Yalantis:Context-Menu.Android:1.0.8")
    implementation("org.adblockplus:adblock-android:3.0")
    implementation("org.adblockplus:adblock-android-settings:3.0")
    implementation("org.adblockplus:adblock-android-webview:3.0")
    implementation("com.just.agentweb:agentweb:+")
    implementation("com.just.agentweb:download:+")
    implementation("com.just.agentweb:filechooser:+")
    implementation("com.yqritc:recyclerview-flexibledivider:+")
    implementation("com.github.piasy:BigImageViewer:+")
    implementation("com.github.piasy:FrescoImageLoader:+")
    implementation("com.github.piasy:FrescoImageViewFactory:+")
    implementation("com.github.gnastnosaj:AndroidTagGroup:743b9e1175")
    implementation("com.github.developer-shivam:Crescento:+")
    implementation("tm.charlie.androidlib:expandable-textview:+")
    implementation("com.jaeger.ninegridimageview:library:+")
    implementation("com.github.iielse:ImageWatcher:+")
    implementation("net.qiujuer.genius:graphics:+")
    implementation("me.drakeet.support:about:+")
    implementation("me.weishu:epic:0.3.6")

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