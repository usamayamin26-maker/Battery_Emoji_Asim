# ProGuard / R8 rules for JH Battery (release: isMinifyEnabled + isShrinkResources).
# Most libraries (AndroidX, Firebase, Play services ads, mediation adapters, OkHttp, Lottie,
# kotlinx.serialization, coroutines) ship their own consumer rules; only gaps are covered here.

# ── Crash reports ─────────────────────────────────────────────────────────────
# Keep file/line info so Crashlytics stack traces map back to source (with mapping.txt).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception

# Annotations/generics needed by serialization and reflection-based libraries
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ── kotlinx.serialization: remote manifest + ads config models ────────────────
# The library's rules cover generated serializers; these keep our @Serializable models and
# their companions intact, so the Dropbox manifest and Remote Config JSON always parse.
-keepclassmembers @kotlinx.serialization.Serializable class com.asim.jhbatteryapp.** {
    *** Companion;
    static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
    <fields>;
}
-keep,includedescriptorclasses class com.asim.jhbatteryapp.**$$serializer { *; }
-keepclasseswithmembers class com.asim.jhbatteryapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Ads ───────────────────────────────────────────────────────────────────────
# The resume app-open ad is skipped over activities matched by simple class name
# (AdsSetup.noResumeAdOver); manifest activities keep their names, this makes it explicit.
-keepnames class * extends android.app.Activity

# Pangle (mediation) references internal classes it doesn't bundle; harmless at runtime.
-dontwarn com.bytedance.sdk.**

# ── Remote asset rendering ────────────────────────────────────────────────────
# AndroidSVG draws remote SVG emojis/stickers/battery icons; it ships no consumer rules.
-keep class com.caverock.androidsvg.** { *; }
-dontwarn com.caverock.androidsvg.**

# BlurView optionally references RenderScript support classes.
-dontwarn eightbitlab.com.blurview.**

# ── Services the system binds by class name ───────────────────────────────────
# Accessibility/overlay services are compared by class name in Settings.Secure strings.
-keepnames class com.asim.jhbatteryapp.service.** extends android.app.Service
-keepnames class com.asim.jhbatteryapp.service.** extends android.accessibilityservice.AccessibilityService
