plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.jhw.batteryemoji"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.jhw.batteryemoji"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Remote asset management — manifest URL injected at build time.
        // Dropbox link must end in dl=1 (direct download), not dl=0 (preview page).
        // Never hardcode secret tokens; use public raw-download share links only.
        buildConfigField(
            "String",
            "ASSET_MANIFEST_URL",
            "\"https://www.dropbox.com/scl/fi/9eldsurxtnau1035me9ii/manifest.json?rlkey=kwnymyenbj0n0r0arhtd3flwm&st=j2ub02sh&dl=0\""
        )
    }

    buildTypes {
        release {
            // R8: removes unused code and obfuscates; shrinkResources then drops unused resources
            isMinifyEnabled = true
            isShrinkResources = true
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
base {
    archivesName = "Battery_emoji_Vcode_${android.defaultConfig.versionCode}_Vname_${android.defaultConfig.versionName}"
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

    // Remote asset management — OkHttp for manifest + asset downloads
    implementation(libs.okhttp)
    // SVG decoding for remote assets (BitmapFactory cannot decode SVG)
    implementation(libs.androidsvg)

    // Dependency injection
    implementation(libs.koin.android)

    // ---------- AdMob mediation ----------
    implementation("com.unity3d.ads:unity-ads:4.21.0")
    implementation("com.google.ads.mediation:unity:4.21.0.0")
    implementation("com.google.ads.mediation:mintegral:17.1.81.1")
    implementation("com.google.ads.mediation:pangle:8.3.0.4.0")
    implementation("com.google.ads.mediation:facebook:6.22.0.1")   // Meta Audience Network
    implementation("com.google.ads.mediation:applovin:13.6.4.2")   // AppLovin network, via AdMob
    implementation("com.yandex.ads.adapter:admob-mobileads:8.6.0.0")
}
