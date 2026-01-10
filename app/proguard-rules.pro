-keep class com.udroid.app.** { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# Suppress warnings for Gradle/build-time classes
-dontwarn org.gradle.api.Plugin
-dontwarn org.jetbrains.kotlin.gradle.**
