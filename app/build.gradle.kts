import org.gradle.kotlin.dsl.implementation

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.github.triplet.play") version "3.8.4"
    id("kotlin-parcelize")
}

android {
    compileSdk = 35
    defaultConfig {
        applicationId = "com.swedbankpay.exampleapp"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "boolean", "ENABLE_PROD_DEMO",
            findProperty("enableProdDemo")?.toString().toBoolean().toString()
        )
        manifestPlaceholders["swedbankPaymentUrlScheme"] = "swedbankexample"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            System.getenv("UPLOAD_KEYSTORE_PATH")?.let { keystorePath ->
                signingConfig = signingConfigs.create("release") {
                    storeFile = File(keystorePath)
                    storePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("UPLOAD_KEY_ALIAS")
                    keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
                }
            }
        }
    }
    namespace = "com.swedbankpay.exampleapp"
}

dependencies {
    implementation(kotlin("reflect"))

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")

    val coroutines_version = "1.9.0"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

    val mobilesdk_local_version = "4.1.1-181-g0c0cdb7-dirty-SNAPSHOT"
    val mobilesdk_version = "5.0.5"
    implementation("com.swedbankpay.mobilesdk:mobilesdk:$mobilesdk_version")
    implementation("com.swedbankpay.mobilesdk:mobilesdk-merchantbackend:$mobilesdk_version")

    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.6")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    val camerax_version = "1.4.1"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("androidx.camera:camera-mlkit-vision:1.4.1")

    implementation("com.google.android.material:material:1.12.0")

    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("junit:junit:4.13.2")
}

play {
    resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.AUTO)
}

// Include version code in version name
androidComponents {
    onVariants { variant ->
        for (output in variant.outputs) {
            val baseVersionName = output.versionName.orNull
            output.versionName.set(output.versionCode.map {
                "$baseVersionName ($it)"
            })
        }
    }
}