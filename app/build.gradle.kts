plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.asim.jhbatteryapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.asim.jhbatteryapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.sdp.android)
    implementation(libs.ssp.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.intuit.ssp:ssp-android:1.1.1")
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.github.Dimezis:BlurView:version-2.0.6")
    implementation("com.airbnb.android:lottie:6.7.1")

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // admob-ads
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Google Play in-app updates (utils/InAppUpdateManager).
    implementation(libs.play.app.update)
    implementation(libs.shimmer)
    implementation(libs.kotlinx.serialization.json)

    // ---------- AdMob mediation ----------
    implementation("com.unity3d.ads:unity-ads:4.21.0")
    implementation("com.google.ads.mediation:unity:4.21.0.0")
    implementation("com.google.ads.mediation:mintegral:17.1.81.1")
    implementation("com.google.ads.mediation:pangle:8.3.0.4.0")
    implementation("com.google.ads.mediation:facebook:6.22.0.1")   // Meta Audience Network
    implementation("com.google.ads.mediation:applovin:13.6.4.2")   // AppLovin network, via AdMob
    implementation("com.yandex.ads.adapter:admob-mobileads:8.6.0.0")
}