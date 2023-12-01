// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
    kotlin("android") version "1.9.10" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        /* Uncomment to build against snapshots
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
         */
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
