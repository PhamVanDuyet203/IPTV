plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.iptv.smart.player.player.streamtv.live.watch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.iptv.smart.player.player.streamtv.live.watch"
        minSdk = 28
        targetSdk = 35
        versionCode = 100
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas")
                argument("room.incremental", "true")
                argument("room.expandProjection", "true")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures{
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    base{
        archivesName = "iptv${version}"
    }
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
//    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    //for exoplayer
    implementation(libs.exoplayerCore)
    implementation(libs.exoplayerUi)

    //for playing online content
    implementation(libs.exoplayerDash)
//    implementation(libs.androidx.fragment)
    implementation(libs.cronet.embedded)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Fragment library
    implementation("androidx.fragment:fragment-ktx:1.6.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.google.code.gson:gson:2.8.8")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")


    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0") // For older versions of LiveData



    // Optional - Coroutines support for Room
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")

    implementation("androidx.paging:paging-runtime:3.2.1")

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    // okhttp
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation ("com.intuit.sdp:sdp-android:1.1.1")

    // AdMob Mediation
    implementation("com.github.thienlp201097:DktechLib:1.4.1")
    implementation("com.google.android.gms:play-services-ads:22.6.0")

    implementation("com.google.ads.mediation:pangle:6.5.0.4.0")
    implementation("com.google.ads.mediation:applovin:13.1.0.0")
    implementation("com.google.ads.mediation:facebook:6.18.0.0")
    implementation("com.google.ads.mediation:mintegral:16.8.61.0")
    implementation("com.google.ads.mediation:ironsource:8.5.0.0")
    implementation ("com.google.ads.mediation:vungle:6.12.1.1")

//Adjust
    implementation ("com.adjust.sdk:adjust-android:5.0.0")
    implementation ("com.android.installreferrer:installreferrer:2.2")
    implementation ("com.google.android.gms:play-services-ads-identifier:18.1.0")
    implementation ("com.adjust.sdk:adjust-android-webbridge:5.0.0")
    //CMP
    implementation ("com.google.android.ump:user-messaging-platform:3.0.0")

    implementation ("com.github.thienlp201097:smart-app-rate:1.0.7")

    implementation ("com.facebook.android:facebook-android-sdk:[8,9)")

//     Firebase
    implementation(libs.firebase.messaging)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.config)



    implementation ("com.airbnb.android:lottie:6.4.0")

    implementation ("com.github.bumptech.glide:glide:4.12.0")
}