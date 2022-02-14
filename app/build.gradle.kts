plugins {
    id("com.android.application")
    kotlin("android")
    id("com.github.triplet.play") version "3.7.0"
}

android {
    compileSdk = 31
    defaultConfig {
        applicationId = "com.swedbankpay.exampleapp"
        minSdk = 21
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("boolean", "ENABLE_PROD_DEMO",
            findProperty("enableProdDemo")?.toString().toBoolean().toString()
        )
    }
    buildFeatures {
        viewBinding = true
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

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
}

dependencies {
    implementation(kotlin("reflect"))

    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.core:core-ktx:1.7.0")

    val coroutines_version = "1.6.0"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

    val mobilesdk_version = "3.1.0"
    implementation("com.swedbankpay.mobilesdk:mobilesdk:$mobilesdk_version")
    implementation("com.swedbankpay.mobilesdk:mobilesdk-merchantbackend:$mobilesdk_version")

    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.4.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.4.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    implementation("com.google.android.material:material:1.5.0")

    implementation("com.google.code.gson:gson:2.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
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