import org.gradle.kotlin.dsl.repositories
import java.net.URI

ext {
    set("kotlinVersion", "1.2.30")

    set("compileSdkVersion", 27)
    set("buildToolsVersion", "27.0.3")
    set("minSdkVersion", 16)
    set("targetSdkVersion", 27)
    set("versionCode", 3)
    set("versionName", "0.0.3")
    set("supportLibraryVersion", "27.1.0")

    set("ankoVersion", "0.10.4")

    set("boilerplateVersion", "2.0.6-rc2")
}

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.1.2")
        classpath("org.codehaus.groovy:groovy-android-gradle-plugin:2.0.0")
    }
}

plugins {
    kotlin("jvm").version("1.2.30").apply(false)
}

allprojects {
    repositories {
        flatDir {
            dirs("libs")
        }
        maven {
            url = URI("https://dl.bintray.com/thelasterstar/maven/")
        }
        maven {
            url = URI("http://dl.bintray.com/piasy/maven")
        }
        maven {
            url = URI("https://jitpack.io")
        }
        google()
        mavenCentral()
        jcenter()
    }
}