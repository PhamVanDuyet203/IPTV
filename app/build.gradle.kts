plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
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
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures{
        viewBinding = true
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
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    //for exoplayer
    implementation(libs.exoplayerCore)
    implementation(libs.exoplayerUi)

    //for playing online content
    implementation(libs.exoplayerDash)
    implementation(libs.androidx.fragment)
    implementation(libs.cronet.embedded)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Fragment library
    implementation("androidx.fragment:fragment-ktx:1.6.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.google.code.gson:gson:2.8.8")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")

//    implementation ("com.github.halilozercan:BetterVideoPlayer:2.0.0-alpha01")
//    implementation ("com.github.halilozercan:BetterVideoPlayer:v1.1.0")
//    implementation ("com.github.halilozercan:BetterVideoPlayer:1.1.0")

//    implementation(project(mapOf("path" to ":bettervideoplayer")))

    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0") // For older versions of LiveData
    // or
//    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.7.1" )// For the latest versions with Kotlin extensions
//    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.1") // For ViewModel
    // Add other dependencies here

    implementation("androidx.viewpager2:viewpager2:1.0.0")


    // viewpage2
    implementation("com.tbuonomo:dotsindicator:5.1.0")

    //
    implementation("com.google.android.material:material:1.9.0")

    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Optional - Coroutines support for Room
    implementation("androidx.room:room-ktx:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation("androidx.paging:paging-runtime:3.2.1")


    // okhttp
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation ("com.intuit.sdp:sdp-android:1.1.1")

    // AdMob Mediation
    implementation("com.google.ads.mediation:pangle:6.5.0.4.0")
    implementation("com.google.ads.mediation:applovin:13.1.0.1")
    implementation("com.google.ads.mediation:facebook:6.18.0.0")
    implementation("com.google.ads.mediation:vungle:7.4.3.0")
    implementation("com.google.ads.mediation:mintegral:16.8.61.0")

    implementation("com.github.thienlp201097:DktechLib:2.0.0")

//Adjust
    implementation ("com.adjust.sdk:adjust-android:5.0.0")
    implementation ("com.android.installreferrer:installreferrer:2.2")
    implementation ("com.google.android.gms:play-services-ads-identifier:18.1.0")
    implementation ("com.adjust.sdk:adjust-android-webbridge:5.0.0")
    //CMP
    implementation ("com.google.android.ump:user-messaging-platform:3.0.0")

    implementation ("com.github.thienlp201097:smart-app-rate:1.0.7")

    implementation ("com.facebook.android:facebook-android-sdk:[8,9)")
    implementation ("com.google.android.gms:play-services-ads:23.0.0")
    // fire base
    implementation ("com.google.firebase:firebase-bom:33.1.0")
    implementation ("com.airbnb.android:lottie:6.4.0")

    implementation ("com.github.bumptech.glide:glide:4.12.0")
}