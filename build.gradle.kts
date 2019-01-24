import org.gradle.kotlin.dsl.repositories
import java.net.URI

ext {
    set("kotlinVersion", "1.2.60")

    set("compileSdkVersion", 28)
    set("buildToolsVersion", "28.0.3")
    set("minSdkVersion", 16)
    set("targetSdkVersion", 28)
    set("versionCode", 10)
    set("versionName", "0.1.0")
    set("supportLibraryVersion", "28.0.0")

    set("ankoVersion", "0.10.4")

    set("boilerplateVersion", "42b326a")
}

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.2.1")
        classpath("org.codehaus.groovy:groovy-android-gradle-plugin:2.0.0")
    }
}

plugins {
    kotlin("jvm").version("1.2.60").apply(false)
}

allprojects {
    repositories {
        flatDir {
            dirs("libs")
        }
        maven {
            url = URI("http://dl.bintray.com/piasy/maven")
        }
        maven {
            url = URI("https://dl.bintray.com/thelasterstar/maven/")
        }
        google()
        jcenter()
        mavenCentral()
        maven {
            url = URI("https://jitpack.io")
        }
    }
}